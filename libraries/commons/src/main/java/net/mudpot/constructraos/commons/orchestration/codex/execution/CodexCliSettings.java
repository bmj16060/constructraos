package net.mudpot.constructraos.commons.orchestration.codex.execution;

public record CodexCliSettings(
    String command,
    String homePath,
    long timeoutSeconds
) {
}
