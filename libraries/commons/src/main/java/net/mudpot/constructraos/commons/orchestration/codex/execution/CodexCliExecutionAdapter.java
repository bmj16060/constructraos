package net.mudpot.constructraos.commons.orchestration.codex.execution;

import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionActivityInput;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class CodexCliExecutionAdapter implements CodexExecutionAdapter {
    private final CodexCliSettings settings;
    private final CodexExecutionResourceSupport resourceSupport;
    private final CodexCliJsonOutputParser outputParser = new CodexCliJsonOutputParser();

    public CodexCliExecutionAdapter(final CodexCliSettings settings) {
        this.settings = settings;
        this.resourceSupport = new CodexExecutionResourceSupport(settings.defaultWorkingDirectory());
    }

    @Override
    public CodexExecutionResult execute(final CodexExecutionActivityInput input) {
        resourceSupport.sanitizedPrompt(input);

        Path tempCodexHome = null;
        Path schemaFile = null;
        try {
            tempCodexHome = prepareCodexHome();
            schemaFile = materializeSchema();
            final List<String> lines = runCodex(input, tempCodexHome, schemaFile);
            final CodexCliJsonOutputParser.ParsedCodexCliOutput parsed = outputParser.parse(lines);
            if (!parsed.errorMessage().isBlank()) {
                throw new IllegalStateException(parsed.errorMessage());
            }
            return parsed.result();
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Codex execution interrupted.", exception);
        } catch (final IOException exception) {
            throw new RuntimeException("Codex execution failed to start.", exception);
        } finally {
            deleteQuietly(schemaFile);
            deleteRecursivelyQuietly(tempCodexHome);
        }
    }

    private List<String> runCodex(
        final CodexExecutionActivityInput input,
        final Path tempCodexHome,
        final Path schemaFile
    ) throws IOException, InterruptedException {
        final List<String> command = new ArrayList<>();
        command.add(sanitize(settings.command()).isBlank() ? "codex" : sanitize(settings.command()));
        command.add("exec");
        command.add("--json");
        command.add("--skip-git-repo-check");
        command.add("--ephemeral");
        command.add("--cd");
        command.add(resourceSupport.resolvedWorkingDirectory(input));
        command.add("--output-schema");
        command.add(schemaFile.toString());
        command.add(resourceSupport.renderPrompt(input));

        final ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().put("CODEX_HOME", tempCodexHome.toString());

        final Process process = processBuilder.start();
        final CopyOnWriteArrayList<String> lines = new CopyOnWriteArrayList<>();
        final Thread reader = Thread.ofVirtual().start(() -> readLines(process, lines));

        final boolean finished = process.waitFor(Duration.ofSeconds(settings.timeoutSeconds()).toSeconds(), TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            reader.join(TimeUnit.SECONDS.toMillis(5));
            throw new IllegalStateException("Codex execution timed out.");
        }

        reader.join(TimeUnit.SECONDS.toMillis(5));
        if (process.exitValue() != 0) {
            final CodexCliJsonOutputParser.ParsedCodexCliOutput parsed = outputParser.parse(lines);
            final String error = parsed.errorMessage().isBlank()
                ? "Codex execution exited with code " + process.exitValue() + "."
                : parsed.errorMessage();
            throw new IllegalStateException(error);
        }
        return List.copyOf(lines);
    }

    private void readLines(final Process process, final CopyOnWriteArrayList<String> lines) {
        try (BufferedReader reader = process.inputReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (final IOException exception) {
            lines.add("{\"type\":\"turn.failed\",\"error\":{\"message\":\"Failed reading Codex output.\"}}");
        }
    }

    private Path prepareCodexHome() throws IOException {
        final Path tempHome = Files.createTempDirectory("constructraos-codex-home-");
        copyIfPresent(resolvedSourceCodexHome().resolve("auth.json"), tempHome.resolve("auth.json"));
        copyIfPresent(resolvedSourceCodexHome().resolve("config.toml"), tempHome.resolve("config.toml"));
        return tempHome;
    }

    private Path materializeSchema() throws IOException {
        final Path schemaFile = Files.createTempFile("constructraos-codex-schema-", ".json");
        Files.writeString(schemaFile, resourceSupport.schemaJson());
        return schemaFile;
    }

    private Path resolvedSourceCodexHome() {
        final String configured = sanitize(settings.homePath());
        if (!configured.isBlank()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.home"), ".codex").toAbsolutePath().normalize();
    }

    private static void copyIfPresent(final Path source, final Path target) throws IOException {
        if (!Files.exists(source)) {
            return;
        }
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
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

    private static void deleteRecursivelyQuietly(final Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(CodexCliExecutionAdapter::deleteQuietly);
        } catch (final IOException ignored) {
            // Cleanup failure is non-fatal.
        }
    }

    private static String sanitize(final String value) {
        return value == null ? "" : value.trim();
    }
}
