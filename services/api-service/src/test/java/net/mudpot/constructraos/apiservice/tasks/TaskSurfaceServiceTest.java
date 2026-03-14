package net.mudpot.constructraos.apiservice.tasks;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import net.mudpot.constructraos.clients.model.WorkflowStartResponse;
import net.mudpot.constructraos.clients.system.CodexExecutionWorkflowClient;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;
import net.mudpot.constructraos.commons.policy.PolicyEvaluator;
import net.mudpot.constructraos.persistence.tasks.TaskStatusQueryService;
import net.mudpot.constructraos.persistence.tasks.TaskStatusView;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskSurfaceServiceTest {
    @Test
    void startTaskNormalizesInputAndDelegatesToWorkflowClient() {
        final StubCodexExecutionWorkflowClient client = new StubCodexExecutionWorkflowClient();
        client.startResponse = new WorkflowStartResponse("CodexExecutionWorkflow", "wf-1", "codex-execution-task-queue", "run-1");
        final CapturingPolicyEvaluator policyEvaluator = new CapturingPolicyEvaluator();
        final TaskSurfaceService service = new TaskSurfaceService(client, new StubTaskStatusQueryService(), policyEvaluator, "");

        final TaskStartResponse response = service.startTask(
            new TaskStartRequest("  Implement TASK-003.  ", "/tmp/project/../project", " reviewer ", " wf-1 "),
            new TaskActorContext("anonymous", "anon-session-1")
        );

        assertEquals("Implement TASK-003.", client.prompt);
        assertEquals("/tmp/project", client.workingDirectory);
        assertEquals("reviewer", client.agentName);
        assertEquals("wf-1", response.workflowId());
        assertEquals("task.codex_execution.start", policyEvaluator.lastRequest.action());
    }

    @Test
    void getTaskStatusRejectsMissingTasks() {
        final TaskSurfaceService service = new TaskSurfaceService(
            new StubCodexExecutionWorkflowClient(),
            new StubTaskStatusQueryService(),
            request -> new PolicyEvaluationResult(true, "allowed", "constructraos.v1"),
            ""
        );

        final HttpStatusException exception = assertThrows(
            HttpStatusException.class,
            () -> service.getTaskStatus("wf-missing", new TaskActorContext("anonymous", "anon-session-1"))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void listTasksReturnsProjectScopedStatuses() {
        final StubTaskStatusQueryService queryService = new StubTaskStatusQueryService();
        queryService.recent = List.of(
            new TaskStatusView(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "wf-2",
                "ConstructraOS",
                "/tmp/project",
                "running",
                "Implement TASK-003.",
                "planner",
                "anonymous",
                "anon-session-1",
                1,
                "running",
                "planner",
                "Working.",
                "reviewer",
                "thread-1",
                null,
                Map.of("status", "running"),
                Instant.parse("2026-03-14T00:00:00Z"),
                null
            )
        );
        final TaskSurfaceService service = new TaskSurfaceService(
            new StubCodexExecutionWorkflowClient(),
            queryService,
            request -> new PolicyEvaluationResult(true, "allowed", "constructraos.v1"),
            ""
        );

        final List<TaskStatusResponse> response = service.listTasks("/tmp/project", 99, new TaskActorContext("anonymous", "anon-session-1"));

        assertEquals(1, response.size());
        assertEquals("/tmp/project", queryService.lastRootPath);
        assertEquals(50, queryService.lastLimit);
        assertEquals("wf-2", response.getFirst().workflowId());
    }

    @Test
    void startTaskUsesConfiguredDefaultWorkingDirectoryWhenBlank() {
        final StubCodexExecutionWorkflowClient client = new StubCodexExecutionWorkflowClient();
        client.startResponse = new WorkflowStartResponse("CodexExecutionWorkflow", "wf-3", "codex-execution-task-queue", "run-3");
        final TaskSurfaceService service = new TaskSurfaceService(
            client,
            new StubTaskStatusQueryService(),
            request -> new PolicyEvaluationResult(true, "allowed", "constructraos.v1"),
            "/workspace"
        );

        service.startTask(
            new TaskStartRequest("Implement TASK-003.", "", "", ""),
            new TaskActorContext("anonymous", "anon-session-1")
        );

        assertEquals("/workspace", client.workingDirectory);
    }

    @Test
    void startTaskRejectsBlankGoal() {
        final TaskSurfaceService service = new TaskSurfaceService(
            new StubCodexExecutionWorkflowClient(),
            new StubTaskStatusQueryService(),
            request -> new PolicyEvaluationResult(true, "allowed", "constructraos.v1"),
            ""
        );

        final HttpStatusException exception = assertThrows(
            HttpStatusException.class,
            () -> service.startTask(new TaskStartRequest(" ", "", "", ""), new TaskActorContext("anonymous", "anon-session-1"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    private static final class StubCodexExecutionWorkflowClient extends CodexExecutionWorkflowClient {
        private String prompt;
        private String workingDirectory;
        private String agentName;
        private WorkflowStartResponse startResponse;

        private StubCodexExecutionWorkflowClient() {
            super(null);
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
            this.prompt = prompt;
            this.workingDirectory = workingDirectory;
            this.agentName = agentName;
            return startResponse;
        }
    }

    private static final class StubTaskStatusQueryService extends TaskStatusQueryService {
        private String lastRootPath;
        private int lastLimit;
        private List<TaskStatusView> recent = List.of();

        private StubTaskStatusQueryService() {
            super(null, null, null, null, null, null);
        }

        @Override
        public Optional<TaskStatusView> findByWorkflowId(final String workflowId) {
            return Optional.empty();
        }

        @Override
        public List<TaskStatusView> recentByProjectRootPath(final String rootPath, final int limit) {
            this.lastRootPath = rootPath;
            this.lastLimit = limit;
            return recent;
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
