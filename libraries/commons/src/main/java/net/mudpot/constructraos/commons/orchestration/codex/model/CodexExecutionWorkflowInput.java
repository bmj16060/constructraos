package net.mudpot.constructraos.commons.orchestration.codex.model;

public record CodexExecutionWorkflowInput(
    String prompt,
    String workingDirectory,
    String agentName,
    String actorKind,
    String sessionId
) {
}
