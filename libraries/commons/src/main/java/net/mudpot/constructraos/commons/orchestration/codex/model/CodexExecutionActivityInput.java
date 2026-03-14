package net.mudpot.constructraos.commons.orchestration.codex.model;

public record CodexExecutionActivityInput(
    String workflowId,
    String prompt,
    String workingDirectory,
    String agentName
) {
}
