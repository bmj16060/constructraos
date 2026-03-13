package net.mudpot.constructraos.commons.projectrecords.model;

public record ProjectExecutionClaimRequest(
    String projectId,
    String executionRequestId,
    String codexThreadId,
    String note
) {
}
