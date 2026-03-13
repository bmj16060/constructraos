package net.mudpot.constructraos.commons.projectrecords.model;

public record ProjectExecutionRequestRecord(
    String id,
    String path,
    String projectId,
    String taskId,
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
