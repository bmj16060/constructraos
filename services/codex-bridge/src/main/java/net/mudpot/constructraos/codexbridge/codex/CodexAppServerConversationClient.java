package net.mudpot.constructraos.codexbridge.codex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchRequest;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchResult;
import net.mudpot.constructraos.codexbridge.callback.CodexExecutionCallbackClient;
import net.mudpot.constructraos.codexbridge.config.CodexAppServerConfig;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class CodexAppServerConversationClient implements CodexConversationClient {
    private static final String CLIENT_NAME = "constructraos-codex-bridge";
    private static final String CLIENT_VERSION = "0.1.0";
    private static final long TURN_OUTCOME_TIMEOUT_MINUTES = 15L;

    private final CodexAppServerConfig config;
    private final CodexAppServerSessionFactory sessionFactory;
    private final CodexExecutionCallbackClient callbackClient;
    private final Path processWorkingDirectory;

    public CodexAppServerConversationClient(
        final CodexAppServerConfig config,
        final ObjectMapper objectMapper,
        final CodexExecutionCallbackClient callbackClient
    ) {
        this(
            config,
            objectMapper,
            new WebSocketCodexAppServerSessionFactory(objectMapper),
            callbackClient,
            Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize()
        );
    }

    CodexAppServerConversationClient(
        final CodexAppServerConfig config,
        final ObjectMapper objectMapper,
        final CodexAppServerSessionFactory sessionFactory,
        final CodexExecutionCallbackClient callbackClient,
        final Path processWorkingDirectory
    ) {
        this.config = config;
        this.sessionFactory = sessionFactory;
        this.callbackClient = callbackClient;
        this.processWorkingDirectory = processWorkingDirectory.toAbsolutePath().normalize();
    }

    @Override
    public CodexExecutionDispatchResult dispatch(final CodexExecutionDispatchRequest request) {
        if (!config.enabled() || config.url().isBlank()) {
            return new CodexExecutionDispatchResult(
                request.executionRequestId(),
                "",
                "dispatched",
                "Codex App Server is not configured yet. Request persisted for bridge consumption."
            );
        }

        final WorkspaceDirectories workspaceDirectories = resolveWorkspaceDirectories(request);
        final boolean resumingThread = !normalize(request.codexThreadId()).isBlank();

        final CodexAppServerSession session = sessionFactory.open(URI.create(config.url()), config.timeout());
        boolean sessionHandedOff = false;
        try {
            initializeSession(session);
            final String threadId = startOrResumeThread(session, request, workspaceDirectories.appServerDirectory(), resumingThread);
            final TurnStartResult turnStartResult = submitTurn(session, threadId, request, workspaceDirectories.appServerDirectory());
            if ("failed".equalsIgnoreCase(turnStartResult.status())) {
                reportFallbackSreOutcome(
                    request,
                    "failed",
                    buildFailedTurnNote(turnStartResult.errorMessage(), "")
                );
                throw new IllegalStateException("Codex App Server reported a failed turn submission.");
            }
            callbackClient.reportAccepted(
                request,
                threadId,
                "Codex bridge submitted the initial specialist turn to Codex App Server."
            );
            if (shouldMonitorTurnOutcome(request)) {
                if (isTerminalTurnStatus(turnStartResult.status())) {
                    reportFallbackSreOutcome(
                        request,
                        "failed",
                        buildCompletedTurnFallbackNote("")
                    );
                } else {
                    sessionHandedOff = true;
                    monitorTurnOutcomeAsync(session, request, turnStartResult.turnId());
                }
            }
            return new CodexExecutionDispatchResult(
                request.executionRequestId(),
                threadId,
                "dispatched",
                buildDispatchNote(workspaceDirectories.appServerDirectory(), resumingThread)
            );
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed dispatching Codex execution request to Codex App Server.", exception);
        } finally {
            if (!sessionHandedOff) {
                session.close();
            }
        }
    }

    private void initializeSession(final CodexAppServerSession session) {
        final Map<String, Object> initializeParams = new LinkedHashMap<>();
        initializeParams.put("clientInfo", Map.of("name", CLIENT_NAME, "version", CLIENT_VERSION));
        initializeParams.put("capabilities", Map.of("experimentalApi", false));
        session.request("initialize", initializeParams, config.timeout());
        session.notify("initialized", null);
    }

    private String startOrResumeThread(
        final CodexAppServerSession session,
        final CodexExecutionDispatchRequest request,
        final Path workspaceDirectory,
        final boolean resumingThread
    ) {
        final Map<String, Object> threadParams = new LinkedHashMap<>();
        threadParams.put("cwd", workspaceDirectory.toString());
        threadParams.put("approvalPolicy", "never");
        threadParams.put("sandbox", normalize(config.sandbox()).isBlank() ? "workspace-write" : normalize(config.sandbox()));
        threadParams.put("model", null);
        threadParams.put("modelProvider", null);
        threadParams.put("config", null);
        threadParams.put("baseInstructions", null);
        threadParams.put("developerInstructions", buildDeveloperInstructions(request, workspaceDirectory));
        threadParams.put("personality", "pragmatic");
        threadParams.put("persistExtendedHistory", false);
        if (resumingThread) {
            threadParams.put("threadId", normalize(request.codexThreadId()));
            final String threadId = session.request("thread/resume", threadParams, config.timeout())
                .path("thread")
                .path("id")
                .asText("");
            if (threadId.isBlank()) {
                throw new IllegalStateException("Codex App Server returned an empty thread ID for thread/resume.");
            }
            return threadId;
        }
        threadParams.put("ephemeral", false);
        threadParams.put("experimentalRawEvents", false);
        final String threadId = session.request("thread/start", threadParams, config.timeout())
            .path("thread")
            .path("id")
            .asText("");
        if (threadId.isBlank()) {
            throw new IllegalStateException("Codex App Server returned an empty thread ID for thread/start.");
        }
        return threadId;
    }

    private TurnStartResult submitTurn(
        final CodexAppServerSession session,
        final String threadId,
        final CodexExecutionDispatchRequest request,
        final Path workspaceDirectory
    ) {
        final Map<String, Object> turnParams = new LinkedHashMap<>();
        turnParams.put("threadId", threadId);
        turnParams.put("input", java.util.List.of(Map.of(
            "type", "text",
            "text", buildUserPrompt(request, workspaceDirectory),
            "text_elements", java.util.List.of()
        )));
        turnParams.put("cwd", workspaceDirectory.toString());
        turnParams.put("approvalPolicy", "never");
        turnParams.put("sandboxPolicy", null);
        turnParams.put("model", null);
        turnParams.put("effort", null);
        turnParams.put("summary", null);
        turnParams.put("personality", "pragmatic");
        turnParams.put("outputSchema", null);
        turnParams.put("collaborationMode", null);

        final JsonNode turn = session.request("turn/start", turnParams, config.timeout()).path("turn");
        return new TurnStartResult(
            turn.path("id").asText(""),
            turn.path("status").asText(""),
            turn.path("error").path("message").asText("")
        );
    }

    private String buildDeveloperInstructions(final CodexExecutionDispatchRequest request, final Path workspaceDirectory) {
        return """
            You are the ConstructraOS %s specialist handling execution request %s.
            Keep work scoped to the local workspace and the repo context at %s.
            Do not perform destructive actions outside the local workspace or local Compose-defined scope without explicit operator approval.
            Use explicit ConstructraOS MCP tools for durable workflow signaling when they are available instead of relying on conversational output as a control channel.
            Keep updates concise and preserve the execution request identifiers in your reasoning and notes.
            """.formatted(
            blankToDefault(request.specialistRole(), "generalist"),
            blankToDefault(request.executionRequestId(), "unknown"),
            workspaceDirectory
        ).trim();
    }

    private String buildUserPrompt(final CodexExecutionDispatchRequest request, final Path workspaceDirectory) {
        return """
            Handle ConstructraOS specialist execution request %s.

            Project: %s
            Task: %s
            Specialist role: %s
            Branch: %s
            Workspace: %s
            Workflow ID: %s
            Acceptance callback signal: %s
            Failure callback signal: %s
            Requested by: %s
            Session ID: %s

            Task instructions:
            %s

            Workflow signaling guidance:
            - The bridge already records initial execution acceptance after the first turn is submitted successfully.
            - If you are reporting an SRE environment result, use the explicit MCP tool `constructra_report_sre_environment_outcome`.
            - Use the exact project ID `%s`, task ID `%s`, and branch name `%s` when calling ConstructraOS MCP tools.
            - Use `constructra_get_task_workflow_state` when you need the current durable workflow state instead of inferring from stale context.

            If you run into missing bridge capabilities, explain the blocker clearly in-thread and preserve the execution request context.
            """.formatted(
            blankToDefault(request.executionRequestId(), "unknown"),
            blankToDefault(request.projectId(), "unknown"),
            blankToDefault(request.taskId(), "unknown"),
            blankToDefault(request.specialistRole(), "unknown"),
            blankToDefault(request.branchName(), "unknown"),
            workspaceDirectory,
            blankToDefault(request.workflowId(), "unknown"),
            blankToDefault(request.callbackSignal(), "unknown"),
            blankToDefault(request.callbackFailureSignal(), "unknown"),
            blankToDefault(request.requestedByKind(), "unknown"),
            blankToDefault(request.sessionId(), "unknown"),
            blankToDefault(request.instructions(), "No additional instructions provided."),
            blankToDefault(request.projectId(), "unknown"),
            blankToDefault(request.taskId(), "unknown"),
            blankToDefault(request.branchName(), "unknown")
        ).trim();
    }

    private WorkspaceDirectories resolveWorkspaceDirectories(final CodexExecutionDispatchRequest request) {
        final Path localWorkspaceDirectory = resolveLocalWorkspaceDirectory(request);
        final Path appServerWorkspaceDirectory = resolveAppServerWorkspaceDirectory(request, localWorkspaceDirectory);
        return new WorkspaceDirectories(localWorkspaceDirectory, appServerWorkspaceDirectory);
    }

    private void monitorTurnOutcomeAsync(
        final CodexAppServerSession session,
        final CodexExecutionDispatchRequest request,
        final String turnId
    ) {
        session.turnOutcome(turnId)
            .orTimeout(TURN_OUTCOME_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .whenComplete((outcome, error) -> {
                try {
                    if (error != null) {
                        reportFallbackSreOutcome(
                            request,
                            "failed",
                            "Codex bridge timed out or lost the App Server session before a durable SRE outcome callback arrived."
                        );
                        return;
                    }
                    final String status = normalize(outcome == null ? "" : outcome.status());
                    if ("completed".equalsIgnoreCase(status)) {
                        reportFallbackSreOutcome(
                            request,
                            "failed",
                            buildCompletedTurnFallbackNote(outcome.lastAgentMessage())
                        );
                    } else if ("failed".equalsIgnoreCase(status)) {
                        reportFallbackSreOutcome(
                            request,
                            "failed",
                            buildFailedTurnNote(outcome.errorMessage(), outcome.lastAgentMessage())
                        );
                    }
                } finally {
                    session.close();
                }
            });
    }

    private void reportFallbackSreOutcome(
        final CodexExecutionDispatchRequest request,
        final String status,
        final String note
    ) {
        if (!"reportSreEnvironmentOutcome".equals(normalize(request.callbackFailureSignal()))) {
            return;
        }
        callbackClient.reportSreEnvironmentOutcome(request, "", status, note);
    }

    private static boolean shouldMonitorTurnOutcome(final CodexExecutionDispatchRequest request) {
        return "reportSreEnvironmentOutcome".equals(normalize(request.callbackFailureSignal()));
    }

    private static boolean isTerminalTurnStatus(final String status) {
        final String normalizedStatus = normalize(status);
        return "completed".equalsIgnoreCase(normalizedStatus) || "failed".equalsIgnoreCase(normalizedStatus);
    }

    private static String buildCompletedTurnFallbackNote(final String lastAgentMessage) {
        final String normalizedLastAgentMessage = normalize(lastAgentMessage);
        if (normalizedLastAgentMessage.isBlank()) {
            return "Codex bridge observed a completed specialist turn without a durable SRE environment callback.";
        }
        return "Codex bridge observed a completed specialist turn without a durable SRE environment callback. Final agent message:\n\n"
            + normalizedLastAgentMessage;
    }

    private static String buildFailedTurnNote(final String errorMessage, final String lastAgentMessage) {
        final String normalizedErrorMessage = normalize(errorMessage);
        final String normalizedLastAgentMessage = normalize(lastAgentMessage);
        if (!normalizedErrorMessage.isBlank() && !normalizedLastAgentMessage.isBlank()) {
            return "Codex bridge observed a failed specialist turn before a durable SRE environment callback. Error: "
                + normalizedErrorMessage + "\n\nLast agent message:\n\n" + normalizedLastAgentMessage;
        }
        if (!normalizedErrorMessage.isBlank()) {
            return "Codex bridge observed a failed specialist turn before a durable SRE environment callback. Error: "
                + normalizedErrorMessage;
        }
        if (!normalizedLastAgentMessage.isBlank()) {
            return "Codex bridge observed a failed specialist turn before a durable SRE environment callback. Last agent message:\n\n"
                + normalizedLastAgentMessage;
        }
        return "Codex bridge observed a failed specialist turn before a durable SRE environment callback.";
    }

    private Path resolveLocalWorkspaceDirectory(final CodexExecutionDispatchRequest request) {
        final Path configuredWorkspaceRoot = resolveConfiguredWorkspaceRoot(request.workspaceRoot());
        final Path workspaceDirectory = resolveWorkspaceDirectory(configuredWorkspaceRoot, request.branchName());
        try {
            Files.createDirectories(workspaceDirectory);
            return workspaceDirectory.toAbsolutePath().normalize();
        } catch (Exception exception) {
            return processWorkingDirectory;
        }
    }

    private Path resolveAppServerWorkspaceDirectory(final CodexExecutionDispatchRequest request, final Path localWorkspaceDirectory) {
        final Path configuredWorkspaceRoot = resolveAppServerWorkspaceRoot();
        if (configuredWorkspaceRoot == null) {
            return localWorkspaceDirectory;
        }
        return resolveWorkspaceDirectory(configuredWorkspaceRoot, request.branchName());
    }

    private Path resolveWorkspaceDirectory(final Path workspaceRoot, final String branchName) {
        if (workspaceRoot == null) {
            return processWorkingDirectory;
        }
        try {
            final String normalizedBranchName = normalize(branchName);
            return normalizedBranchName.isBlank()
                ? workspaceRoot.toAbsolutePath().normalize()
                : workspaceRoot.resolve(Path.of(normalizedBranchName)).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            return processWorkingDirectory;
        }
    }

    private Path resolveConfiguredWorkspaceRoot(final String requestWorkspaceRoot) {
        final String normalizedRequestRoot = normalize(requestWorkspaceRoot);
        if (!normalizedRequestRoot.isBlank()) {
            final Path requestRoot = Path.of(normalizedRequestRoot);
            return requestRoot.isAbsolute()
                ? requestRoot.normalize()
                : processWorkingDirectory.resolve(requestRoot).normalize();
        }
        final String envWorkspaceRoot = normalize(System.getenv("EXECUTION_WORKSPACES_ROOT_DIR"));
        if (!envWorkspaceRoot.isBlank()) {
            return Path.of(envWorkspaceRoot).toAbsolutePath().normalize();
        }
        return null;
    }

    private Path resolveAppServerWorkspaceRoot() {
        final String configuredWorkspaceRoot = normalize(config.workspaceRootDir());
        if (configuredWorkspaceRoot.isBlank()) {
            return null;
        }
        try {
            return Path.of(configuredWorkspaceRoot).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            return null;
        }
    }

    private String buildDispatchNote(final Path workspaceDirectory, final boolean resumingThread) {
        return (resumingThread
            ? "Codex App Server thread resumed and initial turn submitted."
            : "Codex App Server thread started and initial turn submitted.")
            + " Workspace: " + workspaceDirectory + ".";
    }

    private static String blankToDefault(final String value, final String defaultValue) {
        final String normalized = normalize(value);
        return normalized.isBlank() ? defaultValue : normalized;
    }

    private static String normalize(final String value) {
        return value == null ? "" : value.trim();
    }

    private record WorkspaceDirectories(Path localDirectory, Path appServerDirectory) {
    }

    private record TurnStartResult(String turnId, String status, String errorMessage) {
    }
}
