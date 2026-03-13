package net.mudpot.constructraos.orchestrationclients.project;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import net.mudpot.constructraos.commons.orchestration.TaskQueues;
import net.mudpot.constructraos.commons.orchestration.WorkflowNames;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskQaRequestSignal;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskSreEnvironmentOutcomeSignal;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowInput;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowSignalResponse;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowState;
import net.mudpot.constructraos.commons.orchestration.project.workflows.TaskCoordinationWorkflow;

public class TaskCoordinationWorkflowClient {
    private final WorkflowClient workflowClient;

    public TaskCoordinationWorkflowClient(final WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    public TaskWorkflowSignalResponse requestQa(
        final String projectId,
        final String taskId,
        final String branchName,
        final String note,
        final String actorKind,
        final String sessionId
    ) {
        final TaskCoordinationWorkflow workflow = workflowStub(projectId, taskId);
        final WorkflowExecution execution = WorkflowStub.fromTyped(workflow).signalWithStart(
            "requestQa",
            new Object[]{new TaskQaRequestSignal(normalize(branchName), normalize(actorKind), normalize(sessionId), normalize(note))},
            new Object[]{new TaskWorkflowInput(normalize(projectId), normalize(taskId), normalize(actorKind), normalize(sessionId))}
        );
        return new TaskWorkflowSignalResponse(
            WorkflowNames.TASK_COORDINATION,
            execution.getWorkflowId(),
            TaskQueues.TASK_COORDINATION,
            execution.getRunId(),
            "requestQa"
        );
    }

    public TaskWorkflowState currentState(final String projectId, final String taskId) {
        return workflowById(workflowId(projectId, taskId)).currentState();
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
        final TaskCoordinationWorkflow workflow = workflowById(workflowId(projectId, taskId));
        workflow.reportSreEnvironmentOutcome(
            new TaskSreEnvironmentOutcomeSignal(
                normalize(branchName),
                normalize(environmentName),
                normalize(status),
                normalize(actorKind),
                normalize(sessionId),
                normalize(note)
            )
        );
        return new TaskWorkflowSignalResponse(
            WorkflowNames.TASK_COORDINATION,
            workflowId(projectId, taskId),
            TaskQueues.TASK_COORDINATION,
            "",
            "reportSreEnvironmentOutcome"
        );
    }

    private TaskCoordinationWorkflow workflowStub(final String projectId, final String taskId) {
        return workflowClient.newWorkflowStub(
            TaskCoordinationWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(TaskQueues.TASK_COORDINATION)
                .setWorkflowId(workflowId(projectId, taskId))
                .build()
        );
    }

    private TaskCoordinationWorkflow workflowById(final String workflowId) {
        return workflowClient.newWorkflowStub(TaskCoordinationWorkflow.class, workflowId);
    }

    private static String workflowId(final String projectId, final String taskId) {
        return "project-" + normalize(projectId).toLowerCase() + "-task-" + normalize(taskId).toLowerCase();
    }

    private static String normalize(final String value) {
        return value == null ? "" : value.trim();
    }
}
