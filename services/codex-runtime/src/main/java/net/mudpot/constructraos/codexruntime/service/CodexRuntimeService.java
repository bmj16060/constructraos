package net.mudpot.constructraos.codexruntime.service;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.codexruntime.model.CodexRuntimeExecutionRequest;
import net.mudpot.constructraos.codexruntime.model.CodexRuntimeExecutionResponse;
import net.mudpot.constructraos.codexruntime.model.CodexRuntimeHealthResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Singleton
public class CodexRuntimeService implements CodexRuntimeOperations {
    private final String codexCommand;
    private final Path codexHome;
    private final Path configSourcePath;
    private final Path defaultWorkingDirectory;
    private final RuntimeState runtimeState;

    public CodexRuntimeService(
        @Value("${codex.command:codex}") final String codexCommand,
        @Value("${codex.home-path:/codex-home}") final String codexHomePath,
        @Value("${codex.config-source-path:/config}") final String configSourcePath,
        @Value("${codex.default-working-directory:/workspace}") final String defaultWorkingDirectory
    ) {
        this.codexCommand = sanitize(codexCommand).isBlank() ? "codex" : sanitize(codexCommand);
        this.codexHome = Path.of(codexHomePath).toAbsolutePath().normalize();
        this.configSourcePath = Path.of(configSourcePath).toAbsolutePath().normalize();
        this.defaultWorkingDirectory = Path.of(defaultWorkingDirectory).toAbsolutePath().normalize();
        this.runtimeState = initializeRuntime();
    }

    @Override
    public CodexRuntimeHealthResponse health() {
        return new CodexRuntimeHealthResponse(
            runtimeState.configured() ? "ok" : "unconfigured",
            runtimeState.configured(),
            runtimeState.message()
        );
    }

    @Override
    public CodexRuntimeExecutionOutcome execute(final CodexRuntimeExecutionRequest request) {
        if (!runtimeState.configured()) {
            return failure(500, "Codex runtime is not configured: " + runtimeState.message() + ".");
        }

        final String prompt = sanitize(request == null ? null : request.prompt());
        if (prompt.isBlank()) {
            return failure(400, "Prompt is required.");
        }

        final String outputSchema = sanitize(request == null ? null : request.outputSchema());
        if (outputSchema.isBlank()) {
            return failure(400, "output_schema is required.");
        }

        final Path workingDirectory = resolvedWorkingDirectory(request == null ? null : request.workingDirectory());
        if (!Files.exists(workingDirectory)) {
            return failure(400, "Working directory does not exist: " + workingDirectory);
        }
        if (!Files.isDirectory(workingDirectory)) {
            return failure(400, "Working directory is not a directory: " + workingDirectory);
        }

        Path schemaPath = null;
        try {
            schemaPath = Files.createTempFile("constructraos-codex-schema-", ".json");
            Files.writeString(schemaPath, outputSchema, StandardCharsets.UTF_8);
            return new CodexRuntimeExecutionOutcome(
                200,
                runCodex(prompt, workingDirectory, schemaPath, normalizeTimeoutSeconds(request == null ? null : request.timeoutSeconds()))
            );
        } catch (final IOException exception) {
            return failure(500, "Failed to prepare Codex execution: " + exception.getMessage());
        } finally {
            deleteQuietly(schemaPath);
        }
    }

    private RuntimeState initializeRuntime() {
        try {
            Files.createDirectories(codexHome);
            boolean copied = false;
            copied |= copyIfPresent(configSourcePath.resolve("auth.json"), codexHome.resolve("auth.json"));
            copied |= copyIfPresent(configSourcePath.resolve("config.toml"), codexHome.resolve("config.toml"));

            final Path authPath = codexHome.resolve("auth.json");
            final String apiKey = sanitize(System.getenv("OPENAI_API_KEY"));
            if (!apiKey.isBlank() && !Files.exists(authPath)) {
                final CommandResult loginResult = runProcess(List.of(codexCommand, "login", "--with-api-key"), apiKey, Duration.ofSeconds(30));
                if (loginResult.exitCode() != 0) {
                    return new RuntimeState(false, loginResult.error().isBlank()
                        ? "Codex login with API key failed."
                        : loginResult.error());
                }
                return new RuntimeState(true, "configured via OPENAI_API_KEY");
            }
            if (Files.exists(authPath)) {
                return new RuntimeState(true, "configured via explicit auth.json");
            }
            if (copied) {
                return new RuntimeState(false, "config.toml copied but auth.json is missing");
            }
            return new RuntimeState(false, "missing OPENAI_API_KEY or /config/auth.json");
        } catch (final IOException exception) {
            return new RuntimeState(false, "Failed to prepare Codex home: " + exception.getMessage());
        }
    }

    private CodexRuntimeExecutionResponse runCodex(
        final String prompt,
        final Path workingDirectory,
        final Path schemaPath,
        final long timeoutSeconds
    ) {
        final CommandResult result = runProcess(
            List.of(
                codexCommand,
                "exec",
                "--json",
                "--skip-git-repo-check",
                "--ephemeral",
                "--cd",
                workingDirectory.toString(),
                "--output-schema",
                schemaPath.toString(),
                prompt
            ),
            "",
            Duration.ofSeconds(timeoutSeconds)
        );
        return new CodexRuntimeExecutionResponse(result.exitCode(), result.lines(), result.error());
    }

    private CommandResult runProcess(
        final List<String> command,
        final String stdin,
        final Duration timeout
    ) {
        final ProcessBuilder processBuilder = new ProcessBuilder(new ArrayList<>(command));
        processBuilder.environment().put("CODEX_HOME", codexHome.toString());

        try {
            final Process process = processBuilder.start();
            if (!stdin.isBlank()) {
                process.getOutputStream().write(stdin.getBytes(StandardCharsets.UTF_8));
            }
            process.getOutputStream().close();

            final CopyOnWriteArrayList<String> lines = new CopyOnWriteArrayList<>();
            final Thread stdoutReader = Thread.ofVirtual().start(() -> readLines(process.getInputStream(), lines));
            final Thread stderrReader = Thread.ofVirtual().start(() -> readLines(process.getErrorStream(), lines));

            final boolean finished = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                stdoutReader.join(TimeUnit.SECONDS.toMillis(5));
                stderrReader.join(TimeUnit.SECONDS.toMillis(5));
                return new CommandResult(-1, List.of(), "Codex execution timed out.");
            }

            stdoutReader.join(TimeUnit.SECONDS.toMillis(5));
            stderrReader.join(TimeUnit.SECONDS.toMillis(5));
            final String error = process.exitValue() == 0 ? "" : String.join(System.lineSeparator(), lines);
            return new CommandResult(process.exitValue(), List.copyOf(lines), error);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new CommandResult(-1, List.of(), "Codex execution interrupted.");
        } catch (final IOException exception) {
            return new CommandResult(-1, List.of(), "Failed to start Codex: " + exception.getMessage());
        }
    }

    private void readLines(final InputStream stream, final CopyOnWriteArrayList<String> lines) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                final String normalized = sanitize(line);
                if (!normalized.isBlank()) {
                    lines.add(normalized);
                }
            }
        } catch (final IOException ignored) {
            // Best effort capture only.
        }
    }

    private boolean copyIfPresent(final Path source, final Path target) throws IOException {
        if (!Files.exists(source)) {
            return false;
        }
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    private Path resolvedWorkingDirectory(final String workingDirectory) {
        final String normalized = sanitize(workingDirectory);
        return normalized.isBlank()
            ? defaultWorkingDirectory
            : Path.of(normalized).toAbsolutePath().normalize();
    }

    private CodexRuntimeExecutionOutcome failure(final int statusCode, final String message) {
        return new CodexRuntimeExecutionOutcome(statusCode, new CodexRuntimeExecutionResponse(-1, List.of(), message));
    }

    private static long normalizeTimeoutSeconds(final Long timeoutSeconds) {
        if (timeoutSeconds == null || timeoutSeconds <= 0) {
            return 180L;
        }
        return timeoutSeconds;
    }

    private static void deleteQuietly(final Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (final IOException ignored) {
            // Cleanup failure is non-fatal.
        }
    }

    private static String sanitize(final String value) {
        return value == null ? "" : value.trim();
    }

    private record RuntimeState(boolean configured, String message) {
    }

    private record CommandResult(int exitCode, List<String> lines, String error) {
    }
}
