package net.mudpot.constructraos.apiservice.mcp;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.mcp.annotations.Tool;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.apiservice.tasks.TaskActorContext;
import net.mudpot.constructraos.apiservice.tasks.TaskStartRequest;
import net.mudpot.constructraos.apiservice.tasks.TaskStartResponse;
import net.mudpot.constructraos.apiservice.tasks.TaskStatusResponse;
import net.mudpot.constructraos.apiservice.tasks.TaskSurfaceService;

import java.util.List;

@Singleton
public class TaskMcpTool {
    private final TaskSurfaceService taskSurfaceService;

    public TaskMcpTool(final TaskSurfaceService taskSurfaceService) {
        this.taskSurfaceService = taskSurfaceService;
    }

    @Tool(
        name = "task_start",
        description = "Start a Codex-backed task for the active project or an explicit working directory.",
        annotations = @Tool.ToolAnnotations(
            title = "Start Task",
            readOnlyHint = false,
            destructiveHint = false,
            idempotentHint = false,
            openWorldHint = false,
            returnDirect = true
        )
    )
    public TaskStartResponse startTask(
        final String goal,
        @Nullable final String workingDirectory,
        @Nullable final String agentName,
        @Nullable final String workflowId
    ) {
        return taskSurfaceService.startTask(
            new TaskStartRequest(goal, workingDirectory, agentName, workflowId),
            TaskActorContext.mcp()
        );
    }

    @Tool(
        name = "task_status",
        description = "Read the persisted status for a task by workflow ID.",
        annotations = @Tool.ToolAnnotations(
            title = "Task Status",
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false,
            returnDirect = true
        )
    )
    public TaskStatusResponse taskStatus(final String workflowId) {
        return taskSurfaceService.getTaskStatus(workflowId, TaskActorContext.mcp());
    }

    @Tool(
        name = "task_list",
        description = "List recent persisted tasks for the active project or an explicit working directory.",
        annotations = @Tool.ToolAnnotations(
            title = "Recent Tasks",
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false,
            returnDirect = true
        )
    )
    public List<TaskStatusResponse> listTasks(@Nullable final String workingDirectory, @Nullable final Integer limit) {
        return taskSurfaceService.listTasks(workingDirectory, limit == null ? 12 : limit, TaskActorContext.mcp());
    }
}
