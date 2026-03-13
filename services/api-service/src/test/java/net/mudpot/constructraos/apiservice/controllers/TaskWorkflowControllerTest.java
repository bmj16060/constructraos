package net.mudpot.constructraos.apiservice.controllers;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import net.mudpot.constructraos.apiservice.session.AnonymousSession;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionConfig;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionService;
import net.mudpot.constructraos.apiservice.workflow.TaskWorkflowOperationsService;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowSignalResponse;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowState;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;
import net.mudpot.constructraos.commons.policy.PolicyEvaluator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskWorkflowControllerTest {
    @Test
    void requestQaSignalsWorkflowWhenPolicyAllows() {
        final StubTaskWorkflowOperationsService service = new StubTaskWorkflowOperationsService();
        final CapturingPolicyEvaluator policyEvaluator = new CapturingPolicyEvaluator();
        service.response = new TaskWorkflowSignalResponse("TaskCoordinationWorkflow", "wf-task", "task-coordination-task-queue", "run-1", "requestQa");
        final TaskWorkflowController controller = new TaskWorkflowController(service, new StubAnonymousSessionService(), policyEvaluator);

        final TaskWorkflowSignalResponse response = controller.requestQa(
            HttpRequest.POST("/api/projects/constructraos/tasks/T-0001/qa-requests", Map.of()),
            "constructraos",
            "T-0001",
            new TaskWorkflowController.TaskQaRequestBody("project/constructraos/integration", "Run the first QA pass.")
        ).body();

        assertEquals("constructraos", service.projectId);
        assertEquals("T-0001", service.taskId);
        assertEquals("project/constructraos/integration", service.branchName);
        assertEquals("anonymous", service.actorKind);
        assertEquals("anon-session-1", service.sessionId);
        assertEquals("anon-session-1", ((Map<?, ?>) policyEvaluator.lastRequest.input()).get("actor") instanceof Map<?, ?> actor ? actor.get("session_id") : "");
        assertEquals("wf-task", response.workflowId());
    }

    @Test
    void currentStateRejectsDeniedPolicy() {
        final TaskWorkflowController controller = new TaskWorkflowController(
            new StubTaskWorkflowOperationsService(),
            new StubAnonymousSessionService(),
            request -> new PolicyEvaluationResult(false, "denied", "constructraos.v1")
        );

        final HttpStatusException exception = assertThrows(
            HttpStatusException.class,
            () -> controller.currentState(HttpRequest.GET("/api/projects/constructraos/tasks/T-0001/workflow"), "constructraos", "T-0001")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void reportSreEnvironmentOutcomeSignalsWorkflowWhenPolicyAllows() {
        final StubTaskWorkflowOperationsService service = new StubTaskWorkflowOperationsService();
        service.response = new TaskWorkflowSignalResponse("TaskCoordinationWorkflow", "wf-task", "task-coordination-task-queue", "", "reportSreEnvironmentOutcome");
        final TaskWorkflowController controller = new TaskWorkflowController(service, new StubAnonymousSessionService(), new CapturingPolicyEvaluator());

        final TaskWorkflowSignalResponse response = controller.reportSreEnvironmentOutcome(
            HttpRequest.POST("/api/projects/constructraos/tasks/T-0001/sre-environment-outcomes", Map.of()),
            "constructraos",
            "T-0001",
            new TaskWorkflowController.TaskSreEnvironmentOutcomeBody(
                "project/constructraos/integration",
                "branch-env-01",
                "ready",
                "Compose environment rebuilt successfully."
            )
        ).body();

        assertEquals("branch-env-01", service.environmentName);
        assertEquals("ready", service.environmentStatus);
        assertEquals("reportSreEnvironmentOutcome", response.signalName());
    }

    @Test
    void reportCodexExecutionAcceptedSignalsWorkflowWhenPolicyAllows() {
        final StubTaskWorkflowOperationsService service = new StubTaskWorkflowOperationsService();
        service.response = new TaskWorkflowSignalResponse("TaskCoordinationWorkflow", "wf-task", "task-coordination-task-queue", "", "reportCodexExecutionAccepted");
        final TaskWorkflowController controller = new TaskWorkflowController(service, new StubAnonymousSessionService(), new CapturingPolicyEvaluator());

        final TaskWorkflowSignalResponse response = controller.reportCodexExecutionAccepted(
            HttpRequest.POST("/api/projects/constructraos/tasks/T-0001/codex-executions/accepted", Map.of()),
            "constructraos",
            "T-0001",
            new TaskWorkflowController.CodexExecutionAcceptedBody(
                "T-0001-exec-1",
                "codex-thread-123",
                "SRE",
                "Codex accepted the SRE execution request."
            )
        ).body();

        assertEquals("T-0001-exec-1", service.executionRequestId);
        assertEquals("codex-thread-123", service.codexThreadId);
        assertEquals("reportCodexExecutionAccepted", response.signalName());
    }

    private static final class StubTaskWorkflowOperationsService extends TaskWorkflowOperationsService {
        private String projectId;
        private String taskId;
        private String branchName;
        private String note;
        private String actorKind;
        private String sessionId;
        private String environmentName;
        private String environmentStatus;
        private String executionRequestId;
        private String codexThreadId;
        private TaskWorkflowSignalResponse response;

        private StubTaskWorkflowOperationsService() {
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
            return new TaskWorkflowState(projectId, taskId, "OPEN", "in_progress", "SRE", "project/constructraos/integration", "requested", "planned integration environment", "T-0001-exec-1", "codex-thread-123", "E-0001", "qa_requested", 1);
        }

        @Override
        public TaskWorkflowSignalResponse reportSreEnvironmentOutcome(
            final String projectId,
            final String taskId,
            final String branchName,
            final String environmentName,
            final String status,
            final String note,
            final String actorKind,
            final String sessionId
        ) {
            this.projectId = projectId;
            this.taskId = taskId;
            this.branchName = branchName;
            this.environmentName = environmentName;
            this.environmentStatus = status;
            this.note = note;
            this.actorKind = actorKind;
            this.sessionId = sessionId;
            return response;
        }

        @Override
        public TaskWorkflowSignalResponse reportCodexExecutionAccepted(
            final String projectId,
            final String taskId,
            final String executionRequestId,
            final String codexThreadId,
            final String specialistRole,
            final String note
        ) {
            this.projectId = projectId;
            this.taskId = taskId;
            this.executionRequestId = executionRequestId;
            this.codexThreadId = codexThreadId;
            this.actorKind = specialistRole;
            this.note = note;
            return response;
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
