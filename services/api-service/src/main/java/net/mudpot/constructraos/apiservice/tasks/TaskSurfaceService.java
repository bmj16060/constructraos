package net.mudpot.constructraos.apiservice.tasks;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.clients.system.CodexExecutionWorkflowClient;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;
import net.mudpot.constructraos.commons.policy.PolicyEvaluator;
import net.mudpot.constructraos.persistence.tasks.TaskStatusQueryService;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Singleton
public class TaskSurfaceService {
    private static final String DEFAULT_AGENT_NAME = "planner";

    private final CodexExecutionWorkflowClient codexExecutionWorkflowClient;
    private final TaskStatusQueryService taskStatusQueryService;
    private final PolicyEvaluator policyEvaluator;
    private final String defaultWorkingDirectory;

    @Inject
    public TaskSurfaceService(
        final CodexExecutionWorkflowClient codexExecutionWorkflowClient,
        final TaskStatusQueryService taskStatusQueryService,
        final PolicyEvaluator policyEvaluator,
        @Value("${task.default-working-directory:}") final String defaultWorkingDirectory
    ) {
        this.codexExecutionWorkflowClient = codexExecutionWorkflowClient;
        this.taskStatusQueryService = taskStatusQueryService;
        this.policyEvaluator = policyEvaluator;
        this.defaultWorkingDirectory = sanitize(defaultWorkingDirectory);
    }

    public TaskStartResponse startTask(final TaskStartRequest request, final TaskActorContext actor) {
        final TaskStartRequest normalized = normalizedRequest(request);
        requirePolicy(
            "task.codex_execution.start",
            Map.of(
                "actor", actorInput(actor),
                "resource", Map.of("type", "task", "scope", "start"),
                "goal", normalized.goal(),
                "working_directory", normalized.workingDirectory(),
                "agent_name", normalized.agentName()
            )
        );
        return TaskStartResponse.fromWorkflowStartResponse(
            codexExecutionWorkflowClient.start(
                normalized.goal(),
                normalized.workingDirectory(),
                normalized.agentName(),
                normalized.workflowId(),
                sanitize(actor.actorKind()),
                sanitize(actor.sessionId())
            )
        );
    }

    public TaskStatusResponse getTaskStatus(final String workflowId, final TaskActorContext actor) {
        final String normalizedWorkflowId = sanitize(workflowId);
        if (normalizedWorkflowId.isBlank()) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Workflow ID is required.");
        }
        requirePolicy(
            "task.codex_execution.read",
            Map.of(
                "actor", actorInput(actor),
                "resource", Map.of("type", "task", "scope", "status"),
                "workflow_id", normalizedWorkflowId
            )
        );
        return taskStatusQueryService.findByWorkflowId(normalizedWorkflowId)
            .map(TaskStatusResponse::fromView)
            .orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND, "Task not found: " + normalizedWorkflowId));
    }

    public List<TaskStatusResponse> listTasks(final String workingDirectory, final int limit, final TaskActorContext actor) {
        final String normalizedWorkingDirectory = normalizedWorkingDirectory(workingDirectory);
        final int resolvedLimit = Math.max(1, Math.min(limit, 50));
        requirePolicy(
            "task.codex_execution.list",
            Map.of(
                "actor", actorInput(actor),
                "resource", Map.of("type", "task", "scope", "list"),
                "working_directory", normalizedWorkingDirectory,
                "limit", resolvedLimit
            )
        );
        return taskStatusQueryService.recentByProjectRootPath(normalizedWorkingDirectory, resolvedLimit).stream()
            .map(TaskStatusResponse::fromView)
            .toList();
    }

    private void requirePolicy(final String action, final Map<String, Object> input) {
        final PolicyEvaluationResult result = policyEvaluator.evaluate(new PolicyEvaluationRequest(action, input));
        if (!result.allowed()) {
            throw new HttpStatusException(HttpStatus.FORBIDDEN, "Policy denied: " + result.reason());
        }
    }

    private static Map<String, Object> actorInput(final TaskActorContext actor) {
        return Map.of(
            "kind", sanitize(actor == null ? "" : actor.actorKind()),
            "session_id", sanitize(actor == null ? "" : actor.sessionId())
        );
    }

    private TaskStartRequest normalizedRequest(final TaskStartRequest request) {
        if (request == null || sanitize(request.goal()).isBlank()) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Goal is required.");
        }
        return new TaskStartRequest(
            sanitize(request.goal()),
            normalizedWorkingDirectory(request.workingDirectory()),
            normalizedAgentName(request.agentName()),
            sanitize(request.workflowId())
        );
    }

    private String normalizedWorkingDirectory(final String workingDirectory) {
        final String normalized = sanitize(workingDirectory);
        if (normalized.isBlank() && !defaultWorkingDirectory.isBlank()) {
            return Path.of(defaultWorkingDirectory).toAbsolutePath().normalize().toString();
        }
        return normalized.isBlank()
            ? Path.of("").toAbsolutePath().normalize().toString()
            : Path.of(normalized).toAbsolutePath().normalize().toString();
    }

    private static String normalizedAgentName(final String agentName) {
        final String normalized = sanitize(agentName);
        return normalized.isBlank() ? DEFAULT_AGENT_NAME : normalized;
    }

    private static String sanitize(final String value) {
        return value == null ? "" : value.trim();
    }
}
