package net.mudpot.constructraos.apiservice.controllers;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.exceptions.HttpStatusException;
import net.mudpot.constructraos.apiservice.session.AnonymousSession;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionConfig;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionService;
import net.mudpot.constructraos.commons.orchestration.system.model.HelloWorldRequest;
import net.mudpot.constructraos.commons.orchestration.system.model.HelloWorldResult;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;
import net.mudpot.constructraos.commons.policy.PolicyEvaluator;
import net.mudpot.constructraos.clients.model.WorkflowStartResponse;
import net.mudpot.constructraos.clients.system.HelloWorldWorkflowClient;
import net.mudpot.constructraos.persistence.history.HelloHistoryQueryService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HelloWorkflowControllerTest {
    @Test
    void runUsesWorkflowClientWhenPolicyAllows() {
        final StubHelloWorldWorkflowClient client = new StubHelloWorldWorkflowClient();
        final CapturingPolicyEvaluator policyEvaluator = new CapturingPolicyEvaluator();
        client.runResponse = new HelloWorldResult("wf-1", "starter_hello_v1", "Hello there.", "openai-compatible", "demo", Map.of(), Map.of(), Instant.parse("2026-03-12T00:00:00Z"));
        final HelloWorkflowController controller = new HelloWorkflowController(
            client,
            new StubHelloHistoryQueryService(),
            new StubAnonymousSessionService(),
            policyEvaluator
        );

        final HelloWorldResult response = controller.run(
            HttpRequest.POST("/api/workflows/hello-world/run", Map.of()),
            new HelloWorldRequest("Brandon", "Build a club operations platform.", "")
        ).body();

        assertEquals("Brandon", client.runName);
        assertEquals("Build a club operations platform.", client.runUseCase);
        assertEquals("anonymous", client.runActorKind);
        assertEquals("anon-session-1", client.runSessionId);
        assertEquals("Hello there.", response.greeting());
        assertEquals("api.hello_world.run", policyEvaluator.lastRequest.action());
        assertEquals("anon-session-1", ((Map<?, ?>) policyEvaluator.lastRequest.input()).get("actor") instanceof Map<?, ?> actor ? actor.get("session_id") : "");
    }

    @Test
    void historyRejectsDeniedPolicy() {
        final HelloWorkflowController controller = new HelloWorkflowController(
            new StubHelloWorldWorkflowClient(),
            new StubHelloHistoryQueryService(),
            new StubAnonymousSessionService(),
            request -> new PolicyEvaluationResult(false, "denied", "constructraos.v1")
        );

        final HttpStatusException exception = assertThrows(
            HttpStatusException.class,
            () -> controller.history(HttpRequest.GET("/api/workflows/hello-world/history?limit=10"), 10)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    private static final class StubHelloWorldWorkflowClient extends HelloWorldWorkflowClient {
        private String runName;
        private String runUseCase;
        private String runActorKind;
        private String runSessionId;
        private HelloWorldResult runResponse;

        private StubHelloWorldWorkflowClient() {
            super(null);
        }

        @Override
        public HelloWorldResult run(final String name, final String useCase, final String actorKind, final String sessionId) {
            this.runName = name;
            this.runUseCase = useCase;
            this.runActorKind = actorKind;
            this.runSessionId = sessionId;
            return runResponse;
        }

        @Override
        public WorkflowStartResponse start(
            final String name,
            final String useCase,
            final String workflowId,
            final String actorKind,
            final String sessionId
        ) {
            return new WorkflowStartResponse("HelloWorldWorkflow", "wf-1", "hello-world-task-queue", "run-1");
        }
    }

    private static final class StubAnonymousSessionService extends AnonymousSessionService {
        private StubAnonymousSessionService() {
            super(new AnonymousSessionConfig() {
            });
        }

        @Override
        public AnonymousSession ensureSession(final HttpRequest<?> request) {
            return new AnonymousSession("anon-session-1", "anonymous", Instant.parse("2026-03-12T00:00:00Z"), false);
        }
    }

    private static final class StubHelloHistoryQueryService extends HelloHistoryQueryService {
        private StubHelloHistoryQueryService() {
            super(null);
        }

        @Override
        public List<net.mudpot.constructraos.commons.orchestration.system.model.HelloHistoryEntry> recent(final int limit) {
            return List.of();
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
