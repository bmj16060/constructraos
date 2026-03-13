package net.mudpot.constructraos.apiservice.workflow;

import jakarta.inject.Singleton;
import net.mudpot.constructraos.clients.project.TaskCoordinationWorkflowClient;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowSignalResponse;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowState;

@Singleton
public class TaskWorkflowOperationsService {
    public static final String MCP_ACTOR_KIND = "codex";
    public static final String MCP_SESSION_ID = "codex-mcp";

    private final TaskCoordinationWorkflowClient taskCoordinationWorkflowClient;

    public TaskWorkflowOperationsService(final TaskCoordinationWorkflowClient taskCoordinationWorkflowClient) {
        this.taskCoordinationWorkflowClient = taskCoordinationWorkflowClient;
    }

    public TaskWorkflowSignalResponse requestQa(
        final String projectId,
        final String taskId,
        final String branchName,
        final String note,
        final String actorKind,
        final String sessionId
    ) {
        return taskCoordinationWorkflowClient.requestQa(
            projectId,
            taskId,
            branchName,
            note,
            actorKind,
            sessionId
        );
    }

    public TaskWorkflowState currentState(final String projectId, final String taskId) {
        return taskCoordinationWorkflowClient.currentState(projectId, taskId);
    }

    public TaskWorkflowSignalResponse reportSreEnvironmentOutcome(
        final String projectId,
        final String taskId,
        final String branchName,
        final String environmentName,
        final String status,
        final String note,
        final String actorKind,
        final String sessionId
    ) {
        return taskCoordinationWorkflowClient.reportSreEnvironmentOutcome(
            projectId,
            taskId,
            branchName,
            environmentName,
            status,
            note,
            actorKind,
            sessionId
        );
    }

    public TaskWorkflowSignalResponse reportCodexExecutionAccepted(
        final String projectId,
        final String taskId,
        final String executionRequestId,
        final String codexThreadId,
        final String specialistRole,
        final String note
    ) {
        return taskCoordinationWorkflowClient.reportCodexExecutionAccepted(
            projectId,
            taskId,
            executionRequestId,
            codexThreadId,
            specialistRole,
            note
        );
    }
}
