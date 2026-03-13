package net.mudpot.constructraos.commons.orchestration.project.model;

public record CodexExecutionDispatchRequest(
    String projectId,
    String taskId,
    String executionRequestId,
    String specialistRole,
    String branchName,
    String workspaceRoot,
    String workflowId,
    String callbackSignal,
    String callbackFailureSignal,
    String requestedByKind,
    String sessionId,
    String instructions
) {
}
