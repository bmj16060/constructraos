package net.mudpot.constructraos.commons.projectrecords.model;

public record ProjectExecutionRequestWriteRequest(
    String projectId,
    String taskId,
    String executionRequestId,
    String specialistRole,
    String branchName,
    String status,
    String codexThreadId,
    String workflowId,
    String callbackSignal,
    String callbackFailureSignal,
    String note
) {
}
