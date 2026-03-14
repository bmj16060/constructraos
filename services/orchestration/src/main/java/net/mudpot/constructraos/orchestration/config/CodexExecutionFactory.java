package net.mudpot.constructraos.orchestration.config;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.commons.orchestration.codex.execution.CodexCliExecutionAdapter;
import net.mudpot.constructraos.commons.orchestration.codex.execution.CodexCliSettings;
import net.mudpot.constructraos.commons.orchestration.codex.execution.CodexExecutionAdapter;

@Factory
public class CodexExecutionFactory {

    @Singleton
    public CodexExecutionAdapter codexExecutionAdapter(
        @Value("${codex.command:codex}") final String command,
        @Value("${codex.home-path:}") final String homePath,
        @Value("${codex.timeout-seconds:180}") final long timeoutSeconds
    ) {
        return new CodexCliExecutionAdapter(new CodexCliSettings(command, homePath, timeoutSeconds));
    }
}
