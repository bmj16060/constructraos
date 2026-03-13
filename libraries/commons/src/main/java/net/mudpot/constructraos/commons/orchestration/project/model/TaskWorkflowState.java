package net.mudpot.constructraos.commons.orchestration.project.model;

public record TaskWorkflowState(
    String projectId,
    String taskId,
    String workflowStatus,
    String taskStatus,
    String waitingOn,
    String activeBranch,
    String environmentStatus,
    String environmentName,
    String activeExecutionRequestId,
    String codexThreadId,
    String latestEvidenceId,
    String lastEvent,
    int qaRequestCount
) {
}
