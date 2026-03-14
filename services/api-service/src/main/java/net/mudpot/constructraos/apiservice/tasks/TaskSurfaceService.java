package net.mudpot.constructraos.apiservice.tasks;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.apiservice.policy.AuthPolicy;
import net.mudpot.constructraos.clients.system.CodexExecutionWorkflowClient;
import net.mudpot.constructraos.apiservice.tasks.policy.TaskListPolicyInputBuilder;
import net.mudpot.constructraos.apiservice.tasks.policy.TaskReadPolicyInputBuilder;
import net.mudpot.constructraos.apiservice.tasks.policy.TaskStartPolicyInputBuilder;
import net.mudpot.constructraos.apiservice.tasks.policy.TaskTranscriptPolicyInputBuilder;
import net.mudpot.constructraos.persistence.tasks.TaskStatusQueryService;
import net.mudpot.constructraos.persistence.tasks.TaskTranscriptQueryService;

import java.util.List;

@Singleton
public class TaskSurfaceService {
    private final CodexExecutionWorkflowClient codexExecutionWorkflowClient;
    private final TaskStatusQueryService taskStatusQueryService;
    private final TaskTranscriptQueryService taskTranscriptQueryService;
    private final String defaultWorkingDirectory;

    @Inject
    public TaskSurfaceService(
        final CodexExecutionWorkflowClient codexExecutionWorkflowClient,
        final TaskStatusQueryService taskStatusQueryService,
        final TaskTranscriptQueryService taskTranscriptQueryService,
        @Value("${task.default-working-directory:}") final String defaultWorkingDirectory
    ) {
        this.codexExecutionWorkflowClient = codexExecutionWorkflowClient;
        this.taskStatusQueryService = taskStatusQueryService;
        this.taskTranscriptQueryService = taskTranscriptQueryService;
        this.defaultWorkingDirectory = TaskSurfaceNormalization.sanitize(defaultWorkingDirectory);
    }

    @AuthPolicy(value = "api.tasks.start", inputBuilder = TaskStartPolicyInputBuilder.class)
    public TaskStartResponse startTask(final TaskStartRequest request, final TaskActorContext actor) {
        final TaskStartRequest normalized = TaskSurfaceNormalization.normalizedRequest(request, defaultWorkingDirectory);
        return TaskStartResponse.fromWorkflowStartResponse(
            codexExecutionWorkflowClient.start(
                normalized.goal(),
                normalized.workingDirectory(),
                normalized.agentName(),
                normalized.workflowId(),
                TaskSurfaceNormalization.sanitize(actor.actorKind()),
                TaskSurfaceNormalization.sanitize(actor.sessionId())
            )
        );
    }

    @AuthPolicy(value = "api.tasks.read", inputBuilder = TaskReadPolicyInputBuilder.class)
    public TaskStatusResponse getTaskStatus(final String workflowId, final TaskActorContext actor) {
        final String normalizedWorkflowId = TaskSurfaceNormalization.normalizedWorkflowId(workflowId);
        return taskStatusQueryService.findByWorkflowId(normalizedWorkflowId)
            .map(TaskStatusResponse::fromView)
            .orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND, "Task not found: " + normalizedWorkflowId));
    }

    @AuthPolicy(value = "api.tasks.read", inputBuilder = TaskListPolicyInputBuilder.class)
    public List<TaskStatusResponse> listTasks(final String workingDirectory, final int limit, final TaskActorContext actor) {
        final String normalizedWorkingDirectory = TaskSurfaceNormalization.normalizedWorkingDirectory(workingDirectory, defaultWorkingDirectory);
        final int resolvedLimit = TaskSurfaceNormalization.normalizedLimit(limit);
        return taskStatusQueryService.recentByProjectRootPath(normalizedWorkingDirectory, resolvedLimit).stream()
            .map(TaskStatusResponse::fromView)
            .toList();
    }

    @AuthPolicy(value = "api.tasks.read_transcript", inputBuilder = TaskTranscriptPolicyInputBuilder.class)
    public TaskTranscriptResponse getTaskTranscript(final String workflowId, final TaskActorContext actor) {
        final String normalizedWorkflowId = TaskSurfaceNormalization.normalizedWorkflowId(workflowId);
        return taskTranscriptQueryService.findLatestByWorkflowId(normalizedWorkflowId)
            .map(TaskTranscriptResponse::fromView)
            .orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND, "Transcript not found for task: " + normalizedWorkflowId));
    }
}
