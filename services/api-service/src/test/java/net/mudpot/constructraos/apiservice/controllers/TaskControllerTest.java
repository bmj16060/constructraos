package net.mudpot.constructraos.apiservice.controllers;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import net.mudpot.constructraos.apiservice.session.AnonymousSession;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionConfig;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionService;
import net.mudpot.constructraos.apiservice.tasks.TaskActorContext;
import net.mudpot.constructraos.apiservice.tasks.TaskStartRequest;
import net.mudpot.constructraos.apiservice.tasks.TaskStartResponse;
import net.mudpot.constructraos.apiservice.tasks.TaskStatusResponse;
import net.mudpot.constructraos.apiservice.tasks.TaskSurfaceService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskControllerTest {
    @Test
    void startDelegatesToTaskSurfaceService() {
        final StubTaskSurfaceService taskSurfaceService = new StubTaskSurfaceService();
        taskSurfaceService.startResponse = new TaskStartResponse("CodexExecutionWorkflow", "wf-1", "codex-execution-task-queue", "run-1");
        final TaskController controller = new TaskController(taskSurfaceService, new StubAnonymousSessionService());

        final TaskStartResponse response = controller.start(
            HttpRequest.POST("/api/tasks/start", Map.of()),
            new TaskStartRequest("Implement TASK-003.", "/tmp/project", "planner", "")
        ).body();

        assertEquals("Implement TASK-003.", taskSurfaceService.lastStartRequest.goal());
        assertEquals("/tmp/project", taskSurfaceService.lastStartRequest.workingDirectory());
        assertEquals("anonymous", taskSurfaceService.lastActor.actorKind());
        assertEquals("anon-session-1", taskSurfaceService.lastActor.sessionId());
        assertEquals("wf-1", response.workflowId());
    }

    @Test
    void statusPropagatesNotFoundErrors() {
        final StubTaskSurfaceService taskSurfaceService = new StubTaskSurfaceService();
        taskSurfaceService.statusException = new HttpStatusException(HttpStatus.NOT_FOUND, "missing");
        final TaskController controller = new TaskController(taskSurfaceService, new StubAnonymousSessionService());

        final HttpStatusException exception = assertThrows(
            HttpStatusException.class,
            () -> controller.status(HttpRequest.GET("/api/tasks/wf-missing"), "wf-missing")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void recentUsesServiceDefaults() {
        final StubTaskSurfaceService taskSurfaceService = new StubTaskSurfaceService();
        taskSurfaceService.listResponse = List.of(
            new TaskStatusResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "wf-2",
                "ConstructraOS",
                "/tmp/project",
                "completed",
                "Implement TASK-003.",
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
                Map.of("status", "completed"),
                Instant.parse("2026-03-14T00:00:00Z"),
                Instant.parse("2026-03-14T00:02:00Z")
            )
        );
        final TaskController controller = new TaskController(taskSurfaceService, new StubAnonymousSessionService());

        final List<TaskStatusResponse> response = controller.recent(HttpRequest.GET("/api/tasks"), "", 12).body();

        assertEquals("", taskSurfaceService.lastWorkingDirectory);
        assertEquals(12, taskSurfaceService.lastLimit);
        assertEquals(1, response.size());
    }

    private static final class StubTaskSurfaceService extends TaskSurfaceService {
        private TaskStartRequest lastStartRequest;
        private TaskActorContext lastActor;
        private String lastWorkingDirectory;
        private int lastLimit;
        private TaskStartResponse startResponse;
        private List<TaskStatusResponse> listResponse = List.of();
        private HttpStatusException statusException;

        private StubTaskSurfaceService() {
            super(null, null, null, "");
        }

        @Override
        public TaskStartResponse startTask(final TaskStartRequest request, final TaskActorContext actor) {
            this.lastStartRequest = request;
            this.lastActor = actor;
            return startResponse;
        }

        @Override
        public TaskStatusResponse getTaskStatus(final String workflowId, final TaskActorContext actor) {
            if (statusException != null) {
                throw statusException;
            }
            return null;
        }

        @Override
        public List<TaskStatusResponse> listTasks(final String workingDirectory, final int limit, final TaskActorContext actor) {
            this.lastWorkingDirectory = workingDirectory;
            this.lastLimit = limit;
            this.lastActor = actor;
            return listResponse;
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
