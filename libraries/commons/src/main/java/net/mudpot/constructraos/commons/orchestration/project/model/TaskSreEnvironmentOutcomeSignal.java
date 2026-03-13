package net.mudpot.constructraos.commons.orchestration.project.model;

public record TaskSreEnvironmentOutcomeSignal(
    String branchName,
    String environmentName,
    String status,
    String reportedByKind,
    String sessionId,
    String note
) {
}
