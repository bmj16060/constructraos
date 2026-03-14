package net.mudpot.constructraos.apiservice.controllers;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import net.mudpot.constructraos.apiservice.session.AnonymousSession;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionConfig;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionService;
import net.mudpot.constructraos.clients.model.WorkflowStartResponse;
import net.mudpot.constructraos.clients.system.CodexExecutionWorkflowClient;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionRequest;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionResult;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;
import net.mudpot.constructraos.commons.policy.PolicyEvaluator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CodexExecutionWorkflowControllerTest {
    @Test
    void runUsesWorkflowClientWhenPolicyAllows() {
        final StubCodexExecutionWorkflowClient client = new StubCodexExecutionWorkflowClient();
        final CapturingPolicyEvaluator policyEvaluator = new CapturingPolicyEvaluator();
        client.runResponse = new CodexExecutionResult("completed", "Codex returned the structured result.", "none");
        final CodexExecutionWorkflowController controller = new CodexExecutionWorkflowController(
            client,
            new StubAnonymousSessionService(),
            policyEvaluator
        );

        final CodexExecutionResult response = controller.run(
            HttpRequest.POST("/api/workflows/codex-execution/run", Map.of()),
            new CodexExecutionRequest("Summarize the next step.", "/tmp/project", "reviewer", "")
        ).body();

        assertEquals("Summarize the next step.", client.prompt);
        assertEquals("/tmp/project", client.workingDirectory);
        assertEquals("reviewer", client.agentName);
        assertEquals("anonymous", client.actorKind);
        assertEquals("anon-session-1", client.sessionId);
        assertEquals("completed", response.status());
        assertEquals("api.codex_execution.run", policyEvaluator.lastRequest.action());
        assertEquals("anon-session-1", ((Map<?, ?>) policyEvaluator.lastRequest.input()).get("actor") instanceof Map<?, ?> actor ? actor.get("session_id") : "");
    }

    @Test
    void rejectsBlankPrompt() {
        final CodexExecutionWorkflowController controller = new CodexExecutionWorkflowController(
            new StubCodexExecutionWorkflowClient(),
            new StubAnonymousSessionService(),
            request -> new PolicyEvaluationResult(true, "allowed", "constructraos.v1")
        );

        final HttpStatusException exception = assertThrows(
            HttpStatusException.class,
            () -> controller.run(HttpRequest.POST("/api/workflows/codex-execution/run", Map.of()), new CodexExecutionRequest("  ", "", "", ""))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void startRejectsDeniedPolicy() {
        final CodexExecutionWorkflowController controller = new CodexExecutionWorkflowController(
            new StubCodexExecutionWorkflowClient(),
            new StubAnonymousSessionService(),
            request -> new PolicyEvaluationResult(false, "denied", "constructraos.v1")
        );

        final HttpStatusException exception = assertThrows(
            HttpStatusException.class,
            () -> controller.start(
                HttpRequest.POST("/api/workflows/codex-execution/start", Map.of()),
                new CodexExecutionRequest("Summarize the next step.", "", "", "")
            )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    private static final class StubCodexExecutionWorkflowClient extends CodexExecutionWorkflowClient {
        private String prompt;
        private String workingDirectory;
        private String agentName;
        private String actorKind;
        private String sessionId;
        private CodexExecutionResult runResponse;

        private StubCodexExecutionWorkflowClient() {
            super(null);
        }

        @Override
        public CodexExecutionResult run(
            final String prompt,
            final String workingDirectory,
            final String agentName,
            final String actorKind,
            final String sessionId
        ) {
            this.prompt = prompt;
            this.workingDirectory = workingDirectory;
            this.agentName = agentName;
            this.actorKind = actorKind;
            this.sessionId = sessionId;
            return runResponse;
        }

        @Override
        public WorkflowStartResponse start(
            final String prompt,
            final String workingDirectory,
            final String agentName,
            final String workflowId,
            final String actorKind,
            final String sessionId
        ) {
            return new WorkflowStartResponse("CodexExecutionWorkflow", "wf-1", "codex-execution-task-queue", "run-1");
        }
    }

    private static final class StubAnonymousSessionService extends AnonymousSessionService {
        private StubAnonymousSessionService() {
            super(new AnonymousSessionConfig() {
            });
        }

        @Override
        public AnonymousSession ensureSession(final HttpRequest<?> request) {
            return new AnonymousSession("anon-session-1", "anonymous", Instant.parse("2026-03-14T00:00:00Z"), false);
        }
    }

    private static final class CapturingPolicyEvaluator implements PolicyEvaluator {
        private PolicyEvaluationRequest lastRequest;

        @Override
        public PolicyEvaluationResult evaluate(final PolicyEvaluationRequest request) {
            this.lastRequest = request;
            return new PolicyEvaluationResult(true, "allowed", "constructraos.v1");
        }
    }
}
