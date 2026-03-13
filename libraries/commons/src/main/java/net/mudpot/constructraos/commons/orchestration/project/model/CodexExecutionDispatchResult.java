package net.mudpot.constructraos.commons.orchestration.project.model;

public record CodexExecutionDispatchResult(
    String executionRequestId,
    String codexThreadId,
    String status,
    String note
) {
}
