package net.mudpot.constructraos.apiservice.mcp;

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

class TaskMcpToolTest {
    @Test
    void startTaskUsesMcpActorContext() {
        final StubTaskSurfaceService service = new StubTaskSurfaceService();
        service.startResponse = new TaskStartResponse("CodexExecutionWorkflow", "wf-1", "codex-execution-task-queue", "run-1");
        final TaskMcpTool tool = new TaskMcpTool(service);

        final TaskStartResponse response = tool.startTask("Implement TASK-003.", "/tmp/project", "planner", "");

        assertEquals("mcp", service.lastActor.actorKind());
        assertEquals("mcp-tool", service.lastActor.sessionId());
        assertEquals("wf-1", response.workflowId());
    }

    @Test
    void listTasksDelegatesWithDefaultLimit() {
        final StubTaskSurfaceService service = new StubTaskSurfaceService();
        service.listResponse = List.of(
            new TaskStatusResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "wf-2",
                "ConstructraOS",
                "/tmp/project",
                "completed",
                "Implement TASK-003.",
                "planner",
                "mcp",
                "mcp-tool",
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
        final TaskMcpTool tool = new TaskMcpTool(service);

        final List<TaskStatusResponse> response = tool.listTasks("/tmp/project", null);

        assertEquals(12, service.lastLimit);
        assertEquals(1, response.size());
    }

    private static final class StubTaskSurfaceService extends TaskSurfaceService {
        private TaskActorContext lastActor;
        private int lastLimit;
        private TaskStartResponse startResponse;
        private List<TaskStatusResponse> listResponse = List.of();

        private StubTaskSurfaceService() {
            super(null, null, null, "");
        }

        @Override
        public TaskStartResponse startTask(final TaskStartRequest request, final TaskActorContext actor) {
            this.lastActor = actor;
            return startResponse;
        }

        @Override
        public List<TaskStatusResponse> listTasks(final String workingDirectory, final int limit, final TaskActorContext actor) {
            this.lastActor = actor;
            this.lastLimit = limit;
            return listResponse;
        }
    }
}
