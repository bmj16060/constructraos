package net.mudpot.constructraos.orchestration.project.activities;

import jakarta.inject.Singleton;
import net.mudpot.constructraos.commons.orchestration.project.activities.KubectlActivities;
import net.mudpot.constructraos.commons.orchestration.project.model.KubectlCommandRequest;
import net.mudpot.constructraos.commons.orchestration.project.model.KubectlCommandResult;
import net.mudpot.constructraos.orchestration.config.KubectlActivityConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Singleton
public class KubectlActivitiesImpl implements KubectlActivities {
    private final KubectlActivityConfig config;

    public KubectlActivitiesImpl(final KubectlActivityConfig config) {
        this.config = config;
    }

    @Override
    public KubectlCommandResult runCommand(final KubectlCommandRequest request) {
        final List<String> arguments = normalizeArguments(request);
        final List<String> command = new ArrayList<>();
        command.add(config.binary());
        command.addAll(arguments);

        if (!config.enabled()) {
            return new KubectlCommandResult("skipped", command, 0, "", "kubectl activity is disabled");
        }

        final Process process;
        try {
            process = new ProcessBuilder(command).start();
        } catch (IOException exception) {
            return new KubectlCommandResult("failed", command, -1, "", exception.getMessage());
        }

        final CompletableFuture<String> stdoutFuture = readStream(process.getInputStream());
        final CompletableFuture<String> stderrFuture = readStream(process.getErrorStream());
        writeStdin(process, request == null ? "" : request.stdin());

        final Duration timeout = resolveTimeout(request);
        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return new KubectlCommandResult("timed_out", command, -1, await(stdoutFuture), await(stderrFuture));
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return new KubectlCommandResult("failed", command, -1, await(stdoutFuture), exception.getMessage());
        }

        final int exitCode = process.exitValue();
        return new KubectlCommandResult(
            exitCode == 0 ? "succeeded" : "failed",
            command,
            exitCode,
            await(stdoutFuture),
            await(stderrFuture)
        );
    }

    private static List<String> normalizeArguments(final KubectlCommandRequest request) {
        if (request == null || request.arguments() == null || request.arguments().isEmpty()) {
            throw new IllegalArgumentException("kubectl arguments are required");
        }
        final List<String> normalized = request.arguments().stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(argument -> !argument.isEmpty())
            .toList();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("kubectl arguments are required");
        }
        return normalized;
    }

    private Duration resolveTimeout(final KubectlCommandRequest request) {
        if (request == null || request.timeoutSeconds() == null || request.timeoutSeconds() <= 0) {
            return config.timeout();
        }
        return Duration.ofSeconds(request.timeoutSeconds());
    }

    private static void writeStdin(final Process process, final String stdin) {
        try (OutputStream outputStream = process.getOutputStream()) {
            if (stdin != null && !stdin.isBlank()) {
                outputStream.write(stdin.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write kubectl stdin", exception);
        }
    }

    private static CompletableFuture<String> readStream(final InputStream stream) {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStream inputStream = stream) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to read process stream", exception);
            }
        });
    }

    private static String await(final CompletableFuture<String> future) {
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return "";
        } catch (ExecutionException exception) {
            return exception.getCause() == null ? exception.getMessage() : exception.getCause().getMessage();
        }
    }
}
