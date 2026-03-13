package net.mudpot.constructraos.commons.orchestration.project.model;

public record TaskQaRequestSignal(
    String branchName,
    String requestedByKind,
    String sessionId,
    String note
) {
}
