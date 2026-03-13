package net.mudpot.constructraos.commons.orchestration.project.model;

public record TaskWorkflowState(
    String projectId,
    String taskId,
    String workflowStatus,
    String taskStatus,
    String activeBranch,
    String latestEvidenceId,
    String lastEvent,
    int qaRequestCount
) {
}
