package net.mudpot.constructraos.codexbridge.codex;

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

@Singleton
public class CodexAppServerConversationClient implements CodexConversationClient {
    private static final String CLIENT_NAME = "constructraos-codex-bridge";
    private static final String CLIENT_VERSION = "0.1.0";

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

        try (CodexAppServerSession session = sessionFactory.open(URI.create(config.url()), config.timeout())) {
            initializeSession(session);
            final String threadId = startOrResumeThread(session, request, workspaceDirectories.appServerDirectory(), resumingThread);
            submitTurn(session, threadId, request, workspaceDirectories.appServerDirectory());
            callbackClient.reportAccepted(
                request,
                threadId,
                "Codex bridge submitted the initial specialist turn to Codex App Server."
            );
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
        threadParams.put("sandbox", "workspace-write");
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

    private void submitTurn(
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

        final String status = session.request("turn/start", turnParams, config.timeout())
            .path("turn")
            .path("status")
            .asText("");
        if ("failed".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Codex App Server reported a failed turn submission.");
        }
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
}
