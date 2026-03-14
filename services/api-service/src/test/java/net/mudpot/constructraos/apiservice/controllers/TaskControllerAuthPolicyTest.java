package net.mudpot.constructraos.apiservice.controllers;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.apiservice.session.AnonymousSession;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionConfig;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionService;
import net.mudpot.constructraos.apiservice.tasks.TaskActorContext;
import net.mudpot.constructraos.apiservice.tasks.TaskStartRequest;
import net.mudpot.constructraos.apiservice.tasks.TaskStartResponse;
import net.mudpot.constructraos.apiservice.tasks.TaskStatusResponse;
import net.mudpot.constructraos.apiservice.tasks.TaskSurfaceService;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;
import net.mudpot.constructraos.commons.policy.PolicyEvaluator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest(transactional = false)
class TaskControllerAuthPolicyTest {
    @Inject
    TaskController taskController;

    @Inject
    TogglePolicyEvaluator policyEvaluator;

    @Test
    void authPolicyAllowsTaskStartWhenPolicyPasses() {
        policyEvaluator.allowed = true;

        final TaskStartResponse response = taskController.start(
            HttpRequest.POST("/api/tasks/start", Map.of()),
            new TaskStartRequest("Implement auth policy.", "/tmp/project", "planner", "")
        ).body();

        assertEquals("api.tasks.start", policyEvaluator.lastRequest.action());
        assertEquals("CodexExecutionWorkflow", response.workflow());
    }

    @Test
    void authPolicyRejectsTaskReadsWhenPolicyFails() {
        policyEvaluator.allowed = false;

        final HttpStatusException exception = assertThrows(
            HttpStatusException.class,
            () -> taskController.status(HttpRequest.GET("/api/tasks/wf-1"), "wf-1")
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
        @Replaces(TaskSurfaceService.class)
        TaskSurfaceService taskSurfaceService() {
            return new StubTaskSurfaceService();
        }

        @Singleton
        @Replaces(AnonymousSessionService.class)
        AnonymousSessionService anonymousSessionService() {
            return new StubAnonymousSessionService();
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

    static final class StubTaskSurfaceService extends TaskSurfaceService {
        StubTaskSurfaceService() {
            super(null, null, null, "");
        }

        @Override
        public TaskStartResponse startTask(final TaskStartRequest request, final TaskActorContext actor) {
            return new TaskStartResponse("CodexExecutionWorkflow", "wf-1", "codex-execution-task-queue", "run-1");
        }

        @Override
        public TaskStatusResponse getTaskStatus(final String workflowId, final TaskActorContext actor) {
            return new TaskStatusResponse(
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
            );
        }

        @Override
        public List<TaskStatusResponse> listTasks(final String workingDirectory, final int limit, final TaskActorContext actor) {
            return List.of();
        }
    }

    static final class StubAnonymousSessionService extends AnonymousSessionService {
        StubAnonymousSessionService() {
            super(new AnonymousSessionConfig() {
            });
        }

        @Override
        public AnonymousSession ensureSession(final HttpRequest<?> request) {
            final AnonymousSession session = new AnonymousSession("anon-session-1", "anonymous", Instant.parse("2026-03-14T00:00:00Z"), false);
            request.setAttribute("constructraos.anonymous-session", session);
            return session;
        }
    }
}
