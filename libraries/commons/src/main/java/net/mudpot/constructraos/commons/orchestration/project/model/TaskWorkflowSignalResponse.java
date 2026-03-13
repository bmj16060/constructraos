package net.mudpot.constructraos.commons.orchestration.project.model;

public record TaskWorkflowSignalResponse(
    String workflowName,
    String workflowId,
    String taskQueue,
    String runId,
    String signalName
) {
}
