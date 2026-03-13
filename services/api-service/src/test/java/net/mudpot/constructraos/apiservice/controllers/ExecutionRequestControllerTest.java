package net.mudpot.constructraos.apiservice.controllers;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import net.mudpot.constructraos.apiservice.session.AnonymousSession;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionConfig;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionService;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowSignalResponse;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionRequestRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionClaimRequest;
import net.mudpot.constructraos.projectrecords.ProjectRecordsGateway;
import net.mudpot.constructraos.clients.project.TaskCoordinationWorkflowClient;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExecutionRequestControllerTest {
    @Test
    void listExecutionRequestsReturnsPendingRequestsWhenPolicyAllows() {
        final ExecutionRequestController controller = new ExecutionRequestController(
            new StubProjectRecordsGateway(),
            new StubTaskCoordinationWorkflowClient(),
            new StubAnonymousSessionService(),
            request -> new PolicyEvaluationResult(true, "allowed", "constructraos.v1")
        );

        final List<ProjectExecutionRequestRecord> response = controller.listExecutionRequests(
            HttpRequest.GET("/api/projects/constructraos/execution-requests?status=dispatched"),
            "constructraos",
            "dispatched"
        ).body();

        assertEquals(1, response.size());
        assertEquals("T-0001-exec-1", response.get(0).id());
    }

    @Test
    void listExecutionRequestsRejectsDeniedPolicy() {
        final ExecutionRequestController controller = new ExecutionRequestController(
            new StubProjectRecordsGateway(),
            new StubTaskCoordinationWorkflowClient(),
            new StubAnonymousSessionService(),
            request -> new PolicyEvaluationResult(false, "denied", "constructraos.v1")
        );

        final HttpStatusException exception = assertThrows(
            HttpStatusException.class,
            () -> controller.listExecutionRequests(HttpRequest.GET("/api/projects/constructraos/execution-requests"), "constructraos", "")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void claimExecutionRequestClaimsAndSignalsWorkflow() {
        final StubProjectRecordsGateway gateway = new StubProjectRecordsGateway();
        final StubTaskCoordinationWorkflowClient workflowClient = new StubTaskCoordinationWorkflowClient();
        final ExecutionRequestController controller = new ExecutionRequestController(
            gateway,
            workflowClient,
            new StubAnonymousSessionService(),
            request -> new PolicyEvaluationResult(true, "allowed", "constructraos.v1")
        );

        final ProjectExecutionRequestRecord response = controller.claimExecutionRequest(
            HttpRequest.POST("/api/projects/constructraos/execution-requests/T-0001-exec-1/claim", List.of()),
            "constructraos",
            "T-0001-exec-1",
            new ExecutionRequestController.ClaimExecutionRequestBody("codex-thread-123", "SRE", "Claimed by Codex.")
        ).body();

        assertEquals("codex-thread-123", gateway.lastClaim.codexThreadId());
        assertEquals("T-0001-exec-1", workflowClient.executionRequestId);
        assertEquals("codex-thread-123", workflowClient.codexThreadId);
        assertEquals("accepted", response.status());
    }

    private static final class StubProjectRecordsGateway implements ProjectRecordsGateway {
        private ProjectExecutionClaimRequest lastClaim;

        @Override
        public net.mudpot.constructraos.commons.projectrecords.model.ProjectTaskRecord loadTask(final String projectId, final String taskId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public net.mudpot.constructraos.commons.projectrecords.model.ProjectBranchRecord loadBranch(final String projectId, final String branchName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public net.mudpot.constructraos.commons.projectrecords.model.ProjectEnvironmentRecord loadEnvironment(final String projectId, final String environmentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceRecord writeEvidence(final net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceWriteRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public net.mudpot.constructraos.commons.projectrecords.model.ProjectEnvironmentRecord writeEnvironment(final net.mudpot.constructraos.commons.projectrecords.model.ProjectEnvironmentWriteRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProjectExecutionRequestRecord writeExecutionRequest(final net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionRequestWriteRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<net.mudpot.constructraos.commons.projectrecords.model.ProjectEnvironmentRecord> listEnvironments(final String projectId, final String status) {
            return List.of();
        }

        @Override
        public List<ProjectExecutionRequestRecord> listExecutionRequests(final String projectId, final String status) {
            return List.of(new ProjectExecutionRequestRecord("T-0001-exec-1", "/tmp/exec.md", projectId, "T-0001", "SRE", "project/constructraos/integration", "dispatched", "", "project-constructraos-task-t-0001", "reportCodexExecutionAccepted", "reportSreEnvironmentOutcome", "Dispatch request"));
        }

        @Override
        public ProjectExecutionRequestRecord claimExecutionRequest(final ProjectExecutionClaimRequest request) {
            this.lastClaim = request;
            return new ProjectExecutionRequestRecord(
                request.executionRequestId(),
                "/tmp/exec.md",
                request.projectId(),
                "T-0001",
                "SRE",
                "project/constructraos/integration",
                "accepted",
                request.codexThreadId(),
                "project-constructraos-task-t-0001",
                "reportCodexExecutionAccepted",
                "reportSreEnvironmentOutcome",
                request.note()
            );
        }
    }

    private static final class StubTaskCoordinationWorkflowClient extends TaskCoordinationWorkflowClient {
        private String executionRequestId;
        private String codexThreadId;

        private StubTaskCoordinationWorkflowClient() {
            super(null);
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
            this.executionRequestId = executionRequestId;
            this.codexThreadId = codexThreadId;
            return new TaskWorkflowSignalResponse("TaskCoordinationWorkflow", "wf-task", "task-coordination-task-queue", "", "reportCodexExecutionAccepted");
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
}
