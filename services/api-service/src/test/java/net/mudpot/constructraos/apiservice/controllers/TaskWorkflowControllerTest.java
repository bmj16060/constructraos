package net.mudpot.constructraos.apiservice.controllers;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import net.mudpot.constructraos.apiservice.session.AnonymousSession;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionConfig;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionService;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowSignalResponse;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowState;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;
import net.mudpot.constructraos.commons.policy.PolicyEvaluator;
import net.mudpot.constructraos.orchestrationclients.project.TaskCoordinationWorkflowClient;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskWorkflowControllerTest {
    @Test
    void requestQaSignalsWorkflowWhenPolicyAllows() {
        final StubTaskCoordinationWorkflowClient client = new StubTaskCoordinationWorkflowClient();
        final CapturingPolicyEvaluator policyEvaluator = new CapturingPolicyEvaluator();
        client.response = new TaskWorkflowSignalResponse("TaskCoordinationWorkflow", "wf-task", "task-coordination-task-queue", "run-1", "requestQa");
        final TaskWorkflowController controller = new TaskWorkflowController(client, new StubAnonymousSessionService(), policyEvaluator);

        final TaskWorkflowSignalResponse response = controller.requestQa(
            HttpRequest.POST("/api/projects/constructraos/tasks/T-0001/qa-requests", Map.of()),
            "constructraos",
            "T-0001",
            new TaskWorkflowController.TaskQaRequestBody("project/constructraos/integration", "Run the first QA pass.")
        ).body();

        assertEquals("constructraos", client.projectId);
        assertEquals("T-0001", client.taskId);
        assertEquals("project/constructraos/integration", client.branchName);
        assertEquals("anonymous", client.actorKind);
        assertEquals("anon-session-1", client.sessionId);
        assertEquals("anon-session-1", ((Map<?, ?>) policyEvaluator.lastRequest.input()).get("actor") instanceof Map<?, ?> actor ? actor.get("session_id") : "");
        assertEquals("wf-task", response.workflowId());
    }

    @Test
    void currentStateRejectsDeniedPolicy() {
        final TaskWorkflowController controller = new TaskWorkflowController(
            new StubTaskCoordinationWorkflowClient(),
            new StubAnonymousSessionService(),
            request -> new PolicyEvaluationResult(false, "denied", "constructraos.v1")
        );

        final HttpStatusException exception = assertThrows(
            HttpStatusException.class,
            () -> controller.currentState(HttpRequest.GET("/api/projects/constructraos/tasks/T-0001/workflow"), "constructraos", "T-0001")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    private static final class StubTaskCoordinationWorkflowClient extends TaskCoordinationWorkflowClient {
        private String projectId;
        private String taskId;
        private String branchName;
        private String note;
        private String actorKind;
        private String sessionId;
        private TaskWorkflowSignalResponse response;

        private StubTaskCoordinationWorkflowClient() {
            super(null);
        }

        @Override
        public TaskWorkflowSignalResponse requestQa(
            final String projectId,
            final String taskId,
            final String branchName,
            final String note,
            final String actorKind,
            final String sessionId
        ) {
            this.projectId = projectId;
            this.taskId = taskId;
            this.branchName = branchName;
            this.note = note;
            this.actorKind = actorKind;
            this.sessionId = sessionId;
            return response;
        }

        @Override
        public TaskWorkflowState currentState(final String projectId, final String taskId) {
            return new TaskWorkflowState(projectId, taskId, "OPEN", "in_progress", "project/constructraos/integration", "E-0001", "qa_requested", 1);
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

    private static final class CapturingPolicyEvaluator implements PolicyEvaluator {
        private PolicyEvaluationRequest lastRequest;

        @Override
        public PolicyEvaluationResult evaluate(final PolicyEvaluationRequest request) {
            this.lastRequest = request;
            return new PolicyEvaluationResult(true, "allowed", "constructraos.v1");
        }
    }
}
