package net.mudpot.constructraos.codexbridge.codex;

record CodexTurnOutcome(
    String turnId,
    String status,
    String errorMessage,
    String lastAgentMessage
) {
}
