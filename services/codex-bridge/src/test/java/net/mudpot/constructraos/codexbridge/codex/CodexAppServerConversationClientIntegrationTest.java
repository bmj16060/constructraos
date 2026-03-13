package net.mudpot.constructraos.codexbridge.codex;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchRequest;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchResult;
import net.mudpot.constructraos.codexbridge.callback.CodexExecutionCallbackClient;
import net.mudpot.constructraos.codexbridge.config.CodexAppServerConfig;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CodexAppServerConversationClientIntegrationTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void dispatchStartsAndResumesRealCodexThread() throws Exception {
        final Optional<Path> codexExecutable = findExecutable("codex");
        Assumptions.assumeTrue(codexExecutable.isPresent(), "codex executable is not available on PATH.");

        final int port = reservePort();
        final Path tempRoot = Files.createTempDirectory("codex-app-server-it");
        final Path stdoutLog = tempRoot.resolve("codex-app-server.stdout.log");
        final Path stderrLog = tempRoot.resolve("codex-app-server.stderr.log");

        final Process process = new ProcessBuilder(
            codexExecutable.get().toString(),
            "app-server",
            "--listen",
            "ws://127.0.0.1:" + port
        )
            .redirectOutput(stdoutLog.toFile())
            .redirectError(stderrLog.toFile())
            .start();

        try {
            waitForPort("127.0.0.1", port, Duration.ofSeconds(20), stderrLog);

            final CodexAppServerConversationClient client = new CodexAppServerConversationClient(
                enabledConfig(port),
                OBJECT_MAPPER,
                new WebSocketCodexAppServerSessionFactory(OBJECT_MAPPER),
                noOpCallbackClient(),
                tempRoot
            );

            final CodexExecutionDispatchResult started = client.dispatch(request(""));
            assertFalse(started.codexThreadId().isBlank(), "Expected a real Codex thread ID from thread/start.");
            waitForNonEmptyRollout(started.codexThreadId(), Duration.ofSeconds(20));

            final CodexExecutionDispatchResult resumed = client.dispatch(request(started.codexThreadId()));
            assertEquals(started.codexThreadId(), resumed.codexThreadId());
            assertEquals("dispatched", resumed.status());
            assertFalse(Files.notExists(tempRoot.resolve("runtime/workspaces").resolve("project/constructraos/integration")));
        } finally {
            process.destroy();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
            }
        }
    }

    private static CodexExecutionDispatchRequest request(final String codexThreadId) {
        return new CodexExecutionDispatchRequest(
            "constructraos",
            "T-0001",
            "T-0001-exec-1",
            "SRE",
            "project/constructraos/integration",
            "runtime/workspaces",
            "project-constructraos-task-t-0001",
            "reportCodexExecutionAccepted",
            "reportSreEnvironmentOutcome",
            "anonymous",
            "anon-session-1",
            """
                Reply with a short acknowledgement only.
                Do not run shell commands.
                Do not edit files.
                Do not call tools.
                Use the provided thread context only.
                """.trim(),
            codexThreadId
        );
    }

    private static CodexAppServerConfig enabledConfig(final int port) throws Exception {
        final CodexAppServerConfig config = new CodexAppServerConfig();
        set(config, "enabled", true);
        set(config, "url", "ws://127.0.0.1:" + port);
        set(config, "timeoutSeconds", 30);
        return config;
    }

    private static CodexExecutionCallbackClient noOpCallbackClient() {
        return new CodexExecutionCallbackClient() {
            @Override
            public void reportAccepted(final CodexExecutionDispatchRequest request, final String codexThreadId, final String note) {
            }

            @Override
            public void reportSreEnvironmentOutcome(
                final CodexExecutionDispatchRequest request,
                final String environmentName,
                final String status,
                final String note
            ) {
            }
        };
    }

    private static int reservePort() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }

    private static void waitForPort(final String host, final int port, final Duration timeout, final Path stderrLog) throws Exception {
        final Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            try (Socket socket = new Socket(host, port)) {
                return;
            } catch (IOException ignored) {
                Thread.sleep(200L);
            }
        }
        final String stderr = Files.exists(stderrLog) ? Files.readString(stderrLog) : "";
        throw new IllegalStateException("Timed out waiting for codex app-server to listen on port " + port + ". stderr:\n" + stderr);
    }

    private static void waitForNonEmptyRollout(final String threadId, final Duration timeout) throws Exception {
        final Path sessionsRoot = codexSessionsRoot();
        final Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            try (Stream<Path> paths = Files.walk(sessionsRoot)) {
                final boolean ready = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().contains(threadId))
                    .anyMatch(path -> {
                        try {
                            return Files.size(path) > 0L;
                        } catch (IOException exception) {
                            return false;
                        }
                    });
                if (ready) {
                    return;
                }
            }
            Thread.sleep(200L);
        }
        throw new IllegalStateException("Timed out waiting for non-empty rollout file for thread " + threadId + ".");
    }

    private static Path codexSessionsRoot() {
        final String codexHome = System.getenv("CODEX_HOME");
        if (codexHome != null && !codexHome.isBlank()) {
            return Path.of(codexHome).resolve("sessions");
        }
        return Path.of(System.getProperty("user.home")).resolve(".codex").resolve("sessions");
    }

    private static Optional<Path> findExecutable(final String command) {
        final String path = System.getenv("PATH");
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }
        final List<String> roots = Arrays.asList(path.split(java.io.File.pathSeparator));
        for (final String root : roots) {
            final Path candidate = Path.of(root).resolve(command);
            if (Files.isExecutable(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static void set(final Object target, final String fieldName, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
