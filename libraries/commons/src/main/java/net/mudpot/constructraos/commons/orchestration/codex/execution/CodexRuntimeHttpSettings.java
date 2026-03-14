package net.mudpot.constructraos.commons.orchestration.codex.execution;

public record CodexRuntimeHttpSettings(
    String baseUrl,
    long timeoutSeconds,
    String defaultWorkingDirectory
) {
}
