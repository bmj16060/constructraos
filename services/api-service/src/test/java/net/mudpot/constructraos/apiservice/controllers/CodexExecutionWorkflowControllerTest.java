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
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CodexExecutionWorkflowControllerTest {
    @Test
    void runUsesWorkflowClientWhenPolicyAllows() {
        final StubCodexExecutionWorkflowClient client = new StubCodexExecutionWorkflowClient();
        client.runResponse = new CodexExecutionResult("completed", "Codex returned the structured result.", "none");
        final CodexExecutionWorkflowController controller = new CodexExecutionWorkflowController(
            client,
            new StubAnonymousSessionService()
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
    }

    @Test
    void rejectsBlankPrompt() {
        final CodexExecutionWorkflowController controller = new CodexExecutionWorkflowController(
            new StubCodexExecutionWorkflowClient(),
            new StubAnonymousSessionService()
        );

        final HttpStatusException exception = assertThrows(
            HttpStatusException.class,
            () -> controller.run(HttpRequest.POST("/api/workflows/codex-execution/run", Map.of()), new CodexExecutionRequest("  ", "", "", ""))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void startUsesWorkflowClientWhenRequestIsValid() {
        final StubCodexExecutionWorkflowClient client = new StubCodexExecutionWorkflowClient();
        final CodexExecutionWorkflowController controller = new CodexExecutionWorkflowController(
            client,
            new StubAnonymousSessionService()
        );

        final var response = controller.start(
            HttpRequest.POST("/api/workflows/codex-execution/start", Map.of()),
            new CodexExecutionRequest("Summarize the next step.", "", "", "")
        );

        assertEquals("wf-1", response.body().workflowId());
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
}
