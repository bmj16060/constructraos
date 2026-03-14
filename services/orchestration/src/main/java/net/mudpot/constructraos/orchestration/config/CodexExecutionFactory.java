package net.mudpot.constructraos.orchestration.config;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.commons.orchestration.codex.execution.CodexCliExecutionAdapter;
import net.mudpot.constructraos.commons.orchestration.codex.execution.CodexCliSettings;
import net.mudpot.constructraos.commons.orchestration.codex.execution.CodexExecutionAdapter;
import net.mudpot.constructraos.commons.orchestration.codex.execution.CodexRuntimeHttpExecutionAdapter;
import net.mudpot.constructraos.commons.orchestration.codex.execution.CodexRuntimeHttpSettings;

@Factory
public class CodexExecutionFactory {

    @Singleton
    public CodexExecutionAdapter codexExecutionAdapter(
        @Value("${codex.runtime-mode:http}") final String runtimeMode,
        @Value("${codex.runtime-base-url:http://127.0.0.1:8091}") final String runtimeBaseUrl,
        @Value("${codex.command:codex}") final String command,
        @Value("${codex.home-path:}") final String homePath,
        @Value("${codex.timeout-seconds:180}") final long timeoutSeconds,
        @Value("${codex.default-working-directory:}") final String defaultWorkingDirectory
    ) {
        if ("cli".equalsIgnoreCase(sanitize(runtimeMode))) {
            return new CodexCliExecutionAdapter(new CodexCliSettings(command, homePath, timeoutSeconds, defaultWorkingDirectory));
        }
        return new CodexRuntimeHttpExecutionAdapter(new CodexRuntimeHttpSettings(runtimeBaseUrl, timeoutSeconds, defaultWorkingDirectory));
    }

    private static String sanitize(final String value) {
        return value == null ? "" : value.trim();
    }
}
