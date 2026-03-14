package net.mudpot.constructraos.commons.orchestration.codex.execution;

import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionActivityInput;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public final class CodexExecutionResourceSupport {
    private static final String CODEX_SCHEMA_RESOURCE = "codex/execution-result-schema.json";
    private static final String CODEX_PROMPT_RESOURCE = "codex/task-001-execution-prompt.md";

    private final String defaultWorkingDirectory;

    public CodexExecutionResourceSupport(final String defaultWorkingDirectory) {
        this.defaultWorkingDirectory = sanitize(defaultWorkingDirectory);
    }

    public String schemaJson() throws IOException {
        try (var inputStream = requiredClasspathStream(CODEX_SCHEMA_RESOURCE)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public String renderPrompt(final CodexExecutionActivityInput input) throws IOException {
        try (var inputStream = requiredClasspathStream(CODEX_PROMPT_RESOURCE)) {
            final String template = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return template
                .replace("${agent_name}", sanitize(input == null ? null : input.agentName()).isBlank() ? "planner" : sanitize(input.agentName()))
                .replace("${working_directory}", resolvedWorkingDirectory(input))
                .replace("${task_prompt}", sanitizedPrompt(input));
        }
    }

    public String resolvedWorkingDirectory(final CodexExecutionActivityInput input) {
        final String workingDirectory = sanitize(input == null ? null : input.workingDirectory());
        if (workingDirectory.isBlank()) {
            if (!defaultWorkingDirectory.isBlank()) {
                return Path.of(defaultWorkingDirectory).toAbsolutePath().normalize().toString();
            }
            return Path.of("").toAbsolutePath().normalize().toString();
        }
        return Path.of(workingDirectory).toAbsolutePath().normalize().toString();
    }

    public String sanitizedPrompt(final CodexExecutionActivityInput input) {
        final String prompt = sanitize(input == null ? null : input.prompt());
        if (prompt.isBlank()) {
            throw new IllegalArgumentException("Codex execution prompt is required.");
        }
        return prompt;
    }

    private InputStream requiredClasspathStream(final String resourcePath) {
        final InputStream stream = CodexExecutionResourceSupport.class.getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IllegalStateException("Missing required resource: " + resourcePath);
        }
        return stream;
    }

    private static String sanitize(final String value) {
        return value == null ? "" : value.trim();
    }
}
