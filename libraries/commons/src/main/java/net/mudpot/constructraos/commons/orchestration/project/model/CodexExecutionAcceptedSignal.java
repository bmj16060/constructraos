package net.mudpot.constructraos.commons.orchestration.project.model;

public record CodexExecutionAcceptedSignal(
    String executionRequestId,
    String codexThreadId,
    String specialistRole,
    String note
) {
}
