package net.mudpot.constructraos.apiservice.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.mcp.annotations.Resource;
import io.micronaut.mcp.annotations.ResourceTemplate;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.apiservice.policy.AuthPolicy;
import net.mudpot.constructraos.apiservice.tasks.TaskActorContext;
import net.mudpot.constructraos.apiservice.tasks.TaskSurfaceService;

@Singleton
public class TaskMcpResource {
    private final TaskSurfaceService taskSurfaceService;
    private final ObjectMapper objectMapper;

    public TaskMcpResource(
        final TaskSurfaceService taskSurfaceService,
        final ObjectMapper objectMapper
    ) {
        this.taskSurfaceService = taskSurfaceService;
        this.objectMapper = objectMapper;
    }

    @Resource(
        name = "recent_tasks",
        uri = "constructraos://tasks/recent",
        title = "Recent Tasks",
        description = "Recent tasks for the current project context.",
        mimeType = "application/json"
    )
    @AuthPolicy("api.tasks.read")
    public String recentTasks() throws JsonProcessingException {
        return objectMapper.writeValueAsString(taskSurfaceService.listTasks("", 12, TaskActorContext.mcp()));
    }

    @ResourceTemplate(
        name = "task_status_resource",
        uriTemplate = "constructraos://tasks/{workflowId}/status",
        title = "Task Status Resource",
        description = "Persisted task status for a workflow ID.",
        mimeType = "application/json"
    )
    @AuthPolicy("api.tasks.read")
    public String taskStatus(final String workflowId) throws JsonProcessingException {
        return objectMapper.writeValueAsString(taskSurfaceService.getTaskStatus(workflowId, TaskActorContext.mcp()));
    }

    @ResourceTemplate(
        name = "task_transcript_resource",
        uriTemplate = "constructraos://tasks/{workflowId}/transcript",
        title = "Task Transcript Resource",
        description = "Latest persisted transcript for a workflow ID.",
        mimeType = "application/json"
    )
    @AuthPolicy("api.tasks.read_transcript")
    public String taskTranscript(final String workflowId) throws JsonProcessingException {
        return objectMapper.writeValueAsString(taskSurfaceService.getTaskTranscript(workflowId, TaskActorContext.mcp()));
    }
}
