package net.mudpot.constructraos.apiservice.tasks;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.clients.model.WorkflowStartResponse;
import net.mudpot.constructraos.clients.system.CodexExecutionWorkflowClient;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;
import net.mudpot.constructraos.commons.policy.PolicyEvaluator;
import net.mudpot.constructraos.persistence.tasks.TaskStatusQueryService;
import net.mudpot.constructraos.persistence.tasks.TaskStatusView;
import net.mudpot.constructraos.persistence.tasks.TaskTranscriptQueryService;
import net.mudpot.constructraos.persistence.tasks.TaskTranscriptView;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest(transactional = false)
class TaskSurfaceAuthPolicyTest {
    @Inject
    TaskSurfaceService taskSurfaceService;

    @Inject
    TogglePolicyEvaluator policyEvaluator;

    @Test
    void authPolicyAllowsTaskStartWhenPolicyPasses() {
        policyEvaluator.allowed = true;

        final TaskStartResponse response = taskSurfaceService.startTask(
            new TaskStartRequest("Implement auth policy.", "/tmp/project", "planner", ""),
            new TaskActorContext("anonymous", "anon-session-1")
        );

        assertEquals("api.tasks.start", policyEvaluator.lastRequest.action());
        assertEquals("CodexExecutionWorkflow", response.workflow());
    }

    @Test
    void authPolicyRejectsTaskReadsWhenPolicyFails() {
        policyEvaluator.allowed = false;

        final HttpStatusException exception = assertThrows(
            HttpStatusException.class,
            () -> taskSurfaceService.getTaskStatus("wf-1", new TaskActorContext("anonymous", "anon-session-1"))
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("api.tasks.read", policyEvaluator.lastRequest.action());
    }

    @Factory
    static class TestFactory {
        @Singleton
        @Replaces(PolicyEvaluator.class)
        TogglePolicyEvaluator policyEvaluator() {
            return new TogglePolicyEvaluator();
        }

        @Singleton
        @Replaces(CodexExecutionWorkflowClient.class)
        CodexExecutionWorkflowClient codexExecutionWorkflowClient() {
            return new StubCodexExecutionWorkflowClient();
        }

        @Singleton
        @Replaces(TaskStatusQueryService.class)
        TaskStatusQueryService taskStatusQueryService() {
            return new StubTaskStatusQueryService();
        }

        @Singleton
        @Replaces(TaskTranscriptQueryService.class)
        TaskTranscriptQueryService taskTranscriptQueryService() {
            return new StubTaskTranscriptQueryService();
        }
    }

    static final class TogglePolicyEvaluator implements PolicyEvaluator {
        boolean allowed = true;
        PolicyEvaluationRequest lastRequest;

        @Override
        public PolicyEvaluationResult evaluate(final PolicyEvaluationRequest request) {
            this.lastRequest = request;
            return new PolicyEvaluationResult(allowed, allowed ? "allowed" : "denied", "constructraos.v1");
        }
    }

    static final class StubCodexExecutionWorkflowClient extends CodexExecutionWorkflowClient {
        StubCodexExecutionWorkflowClient() {
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
            return new WorkflowStartResponse("CodexExecutionWorkflow", "wf-1", "codex-execution-task-queue", "run-1");
        }
    }

    static final class StubTaskStatusQueryService extends TaskStatusQueryService {
        StubTaskStatusQueryService() {
            super(null, null, null, null, null, null);
        }

        @Override
        public Optional<TaskStatusView> findByWorkflowId(final String workflowId) {
            return Optional.of(
                new TaskStatusView(
                    UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    workflowId,
                    "workspace",
                    "/workspace",
                    "completed",
                    "done",
                    "planner",
                    "anonymous",
                    "anon-session-1",
                    1,
                    "completed",
                    "planner",
                    "Done.",
                    "none",
                    "thread-1",
                    null,
                    Map.of(),
                    Instant.parse("2026-03-14T00:00:00Z"),
                    Instant.parse("2026-03-14T00:01:00Z")
                )
            );
        }
    }

    static final class StubTaskTranscriptQueryService extends TaskTranscriptQueryService {
        StubTaskTranscriptQueryService() {
            super(null, null, null);
        }

        @Override
        public Optional<TaskTranscriptView> findLatestByWorkflowId(final String workflowId) {
            return Optional.of(
                new TaskTranscriptView(
                    UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    workflowId,
                    UUID.fromString("22222222-2222-2222-2222-222222222222"),
                    1,
                    "completed",
                    "planner",
                    "codex-cli-jsonl",
                    "thread-1",
                    List.of(),
                    Instant.parse("2026-03-14T00:00:00Z"),
                    Instant.parse("2026-03-14T00:01:00Z")
                )
            );
        }
    }
}
