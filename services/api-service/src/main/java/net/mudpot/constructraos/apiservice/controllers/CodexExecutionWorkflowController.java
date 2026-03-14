package net.mudpot.constructraos.apiservice.controllers;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import net.mudpot.constructraos.apiservice.session.AnonymousSession;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionService;
import net.mudpot.constructraos.clients.model.WorkflowStartResponse;
import net.mudpot.constructraos.clients.system.CodexExecutionWorkflowClient;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionRequest;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionResult;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;
import net.mudpot.constructraos.commons.policy.PolicyEvaluator;

import java.nio.file.Paths;
import java.util.Map;

@Controller("/api/workflows/codex-execution")
@ExecuteOn(TaskExecutors.BLOCKING)
public class CodexExecutionWorkflowController {
    private static final String DEFAULT_AGENT_NAME = "planner";

    private final CodexExecutionWorkflowClient codexExecutionWorkflowClient;
    private final AnonymousSessionService anonymousSessionService;
    private final PolicyEvaluator policyEvaluator;

    public CodexExecutionWorkflowController(
        final CodexExecutionWorkflowClient codexExecutionWorkflowClient,
        final AnonymousSessionService anonymousSessionService,
        final PolicyEvaluator policyEvaluator
    ) {
        this.codexExecutionWorkflowClient = codexExecutionWorkflowClient;
        this.anonymousSessionService = anonymousSessionService;
        this.policyEvaluator = policyEvaluator;
    }

    @Post("/run")
    public MutableHttpResponse<CodexExecutionResult> run(final HttpRequest<?> httpRequest, @Body final CodexExecutionRequest request) {
        final CodexExecutionRequest normalized = normalizedRequest(request);
        final AnonymousSession session = anonymousSessionService.ensureSession(httpRequest);
        requirePolicy("workflow.codex_execution.run", normalized, session);
        return anonymousSessionService.attachCookieIfNeeded(
            HttpResponse.ok(
                codexExecutionWorkflowClient.run(
                    normalized.prompt(),
                    normalized.workingDirectory(),
                    normalized.agentName(),
                    session.actorKind(),
                    session.sessionId()
                )
            ),
            session
        );
    }

    @Post("/start")
    public MutableHttpResponse<WorkflowStartResponse> start(final HttpRequest<?> httpRequest, @Body final CodexExecutionRequest request) {
        final CodexExecutionRequest normalized = normalizedRequest(request);
        final AnonymousSession session = anonymousSessionService.ensureSession(httpRequest);
        requirePolicy("workflow.codex_execution.start", normalized, session);
        return anonymousSessionService.attachCookieIfNeeded(
            HttpResponse.ok(
                codexExecutionWorkflowClient.start(
                    normalized.prompt(),
                    normalized.workingDirectory(),
                    normalized.agentName(),
                    normalized.workflowId(),
                    session.actorKind(),
                    session.sessionId()
                )
            ),
            session
        );
    }

    private void requirePolicy(final String action, final CodexExecutionRequest request, final AnonymousSession session) {
        final Map<String, Object> input = Map.of(
            "actor", Map.of(
                "kind", session.actorKind(),
                "session_id", session.sessionId()
            ),
            "resource", Map.of("type", "codex-execution", "scope", "workflow"),
            "prompt", request.prompt(),
            "working_directory", request.workingDirectory(),
            "agent_name", request.agentName()
        );
        final PolicyEvaluationResult result = policyEvaluator.evaluate(new PolicyEvaluationRequest(action, input));
        if (!result.allowed()) {
            throw new HttpStatusException(HttpStatus.FORBIDDEN, "Policy denied: " + result.reason());
        }
    }

    private static CodexExecutionRequest normalizedRequest(final CodexExecutionRequest request) {
        if (request == null || sanitize(request.prompt()).isBlank()) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Prompt is required.");
        }
        return new CodexExecutionRequest(
            sanitize(request.prompt()),
            normalizedWorkingDirectory(request.workingDirectory()),
            normalizedAgentName(request.agentName()),
            sanitize(request.workflowId())
        );
    }

    private static String normalizedWorkingDirectory(final String workingDirectory) {
        final String normalized = sanitize(workingDirectory);
        return normalized.isBlank()
            ? Paths.get("").toAbsolutePath().normalize().toString()
            : normalized;
    }

    private static String normalizedAgentName(final String agentName) {
        final String normalized = sanitize(agentName);
        return normalized.isBlank() ? DEFAULT_AGENT_NAME : normalized;
    }

    private static String sanitize(final String value) {
        return value == null ? "" : value.trim();
    }
}
