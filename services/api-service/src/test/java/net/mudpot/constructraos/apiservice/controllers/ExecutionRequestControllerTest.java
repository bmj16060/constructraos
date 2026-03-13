package net.mudpot.constructraos.apiservice.controllers;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import net.mudpot.constructraos.apiservice.session.AnonymousSession;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionConfig;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionService;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionRequestRecord;
import net.mudpot.constructraos.projectrecords.ProjectRecordsGateway;
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
            new StubAnonymousSessionService(),
            request -> new PolicyEvaluationResult(false, "denied", "constructraos.v1")
        );

        final HttpStatusException exception = assertThrows(
            HttpStatusException.class,
            () -> controller.listExecutionRequests(HttpRequest.GET("/api/projects/constructraos/execution-requests"), "constructraos", "")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    private static final class StubProjectRecordsGateway implements ProjectRecordsGateway {
        @Override
        public net.mudpot.constructraos.commons.projectrecords.model.ProjectTaskRecord loadTask(final String projectId, final String taskId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public net.mudpot.constructraos.commons.projectrecords.model.ProjectBranchRecord loadBranch(final String projectId, final String branchName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceRecord writeEvidence(final net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceWriteRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProjectExecutionRequestRecord writeExecutionRequest(final net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionRequestWriteRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ProjectExecutionRequestRecord> listExecutionRequests(final String projectId, final String status) {
            return List.of(new ProjectExecutionRequestRecord("T-0001-exec-1", "/tmp/exec.md", projectId, "T-0001", "SRE", "project/constructraos/integration", "dispatched", "", "project-constructraos-task-t-0001"));
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
