package net.mudpot.constructraos.codexbridge.codex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchRequest;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchResult;
import net.mudpot.constructraos.codexbridge.config.CodexAppServerConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodexAppServerConversationClientTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void dispatchReturnsPlaceholderWhenAppServerDisabled() throws Exception {
        final CodexAppServerConversationClient client = new CodexAppServerConversationClient(
            disabledConfig(),
            OBJECT_MAPPER,
            new FakeSessionFactory((method, params) -> OBJECT_MAPPER.createObjectNode()),
            new CapturingCallbackClient(),
            Path.of(".").toAbsolutePath().normalize()
        );

        final CodexExecutionDispatchResult result = client.dispatch(request(" "));

        assertEquals("T-0001-exec-1", result.executionRequestId());
        assertEquals("dispatched", result.status());
    }

    @Test
    void dispatchStartsThreadAndSubmitsTurn() throws Exception {
        final Path tempRoot = Files.createTempDirectory("codex-bridge-workspaces");
        final List<String> observedMethods = new ArrayList<>();
        final List<JsonNode> observedParams = new ArrayList<>();
        final CapturingCallbackClient callbackClient = new CapturingCallbackClient();
        final CodexAppServerConversationClient client = new CodexAppServerConversationClient(
            enabledConfig(),
            OBJECT_MAPPER,
            new FakeSessionFactory((method, params) -> {
                observedMethods.add(method);
                observedParams.add(params);
                if ("initialize".equals(method)) {
                    return json("{\"userAgent\":\"codex-test\"}");
                }
                if ("thread/start".equals(method)) {
                    return json("{\"thread\":{\"id\":\"thread-123\"}}");
                }
                if ("turn/start".equals(method)) {
                    return json("{\"turn\":{\"id\":\"turn-1\",\"status\":\"inProgress\",\"error\":null,\"items\":[]}}");
                }
                throw new IllegalStateException("Unexpected method " + method);
            }),
            callbackClient,
            tempRoot
        );

        final CodexExecutionDispatchResult result = client.dispatch(request(""));

        assertEquals(List.of("initialize", "thread/start", "turn/start"), observedMethods);
        assertEquals("thread-123", result.codexThreadId());
        assertTrue(result.note().contains("thread started"));
        assertEquals(1, callbackClient.accepted.size());
        assertTrue(callbackClient.accepted.getFirst().startsWith("thread-123|"));
        assertEquals(
            tempRoot.resolve("runtime/workspaces").resolve("project/constructraos/integration").toString(),
            observedParams.get(1).path("cwd").asText()
        );
        assertEquals("workspace-write", observedParams.get(1).path("sandbox").asText());
        assertTrue(observedParams.get(2).path("input").get(0).path("text").asText().contains("Handle ConstructraOS specialist execution request T-0001-exec-1."));
    }

    @Test
    void dispatchUsesConfiguredAppServerWorkspaceRootForThreadCwd() throws Exception {
        final Path localRoot = Files.createTempDirectory("codex-bridge-local-workspaces");
        final Path appServerRoot = Files.createTempDirectory("codex-bridge-app-server-workspaces");
        final List<JsonNode> observedParams = new ArrayList<>();
        final CodexAppServerConversationClient client = new CodexAppServerConversationClient(
            enabledConfig(appServerRoot),
            OBJECT_MAPPER,
            new FakeSessionFactory((method, params) -> {
                observedParams.add(params);
                if ("initialize".equals(method)) {
                    return json("{\"userAgent\":\"codex-test\"}");
                }
                if ("thread/start".equals(method)) {
                    return json("{\"thread\":{\"id\":\"thread-123\"}}");
                }
                if ("turn/start".equals(method)) {
                    return json("{\"turn\":{\"id\":\"turn-1\",\"status\":\"inProgress\",\"error\":null,\"items\":[]}}");
                }
                throw new IllegalStateException("Unexpected method " + method);
            }),
            new CapturingCallbackClient(),
            localRoot
        );

        client.dispatch(request(""));

        assertEquals(
            appServerRoot.resolve("project/constructraos/integration").toString(),
            observedParams.get(1).path("cwd").asText()
        );
        assertTrue(Files.isDirectory(localRoot.resolve("runtime/workspaces").resolve("project/constructraos/integration")));
    }

    @Test
    void dispatchUsesConfiguredSandboxModeForThreadStart() throws Exception {
        final List<JsonNode> observedParams = new ArrayList<>();
        final CodexAppServerConversationClient client = new CodexAppServerConversationClient(
            enabledConfig(null, "danger-full-access"),
            OBJECT_MAPPER,
            new FakeSessionFactory((method, params) -> {
                observedParams.add(params);
                if ("initialize".equals(method)) {
                    return json("{\"userAgent\":\"codex-test\"}");
                }
                if ("thread/start".equals(method)) {
                    return json("{\"thread\":{\"id\":\"thread-123\"}}");
                }
                if ("turn/start".equals(method)) {
                    return json("{\"turn\":{\"id\":\"turn-1\",\"status\":\"inProgress\",\"error\":null,\"items\":[]}}");
                }
                throw new IllegalStateException("Unexpected method " + method);
            }),
            new CapturingCallbackClient(),
            Path.of(".").toAbsolutePath().normalize()
        );

        client.dispatch(request(""));

        assertEquals("danger-full-access", observedParams.get(1).path("sandbox").asText());
    }

    @Test
    void dispatchResumesExistingThreadWhenThreadIdProvided() throws Exception {
        final List<String> observedMethods = new ArrayList<>();
        final CapturingCallbackClient callbackClient = new CapturingCallbackClient();
        final CodexAppServerConversationClient client = new CodexAppServerConversationClient(
            enabledConfig(),
            OBJECT_MAPPER,
            new FakeSessionFactory((method, params) -> {
                observedMethods.add(method);
                if ("initialize".equals(method)) {
                    return json("{\"userAgent\":\"codex-test\"}");
                }
                if ("thread/resume".equals(method)) {
                    return json("{\"thread\":{\"id\":\"thread-existing\"}}");
                }
                if ("turn/start".equals(method)) {
                    return json("{\"turn\":{\"id\":\"turn-2\",\"status\":\"completed\",\"error\":null,\"items\":[]}}");
                }
                throw new IllegalStateException("Unexpected method " + method);
            }),
            callbackClient,
            Path.of(".").toAbsolutePath().normalize()
        );

        final CodexExecutionDispatchResult result = client.dispatch(request("thread-existing"));

        assertEquals(List.of("initialize", "thread/resume", "turn/start"), observedMethods);
        assertEquals("thread-existing", result.codexThreadId());
        assertTrue(result.note().contains("thread resumed"));
        assertEquals(List.of("thread-existing|Codex bridge submitted the initial specialist turn to Codex App Server."), callbackClient.accepted);
    }

    @Test
    void dispatchFailsWhenTurnSubmissionFails() throws Exception {
        final CapturingCallbackClient callbackClient = new CapturingCallbackClient();
        final CodexAppServerConversationClient client = new CodexAppServerConversationClient(
            enabledConfig(),
            OBJECT_MAPPER,
            new FakeSessionFactory((method, params) -> {
                if ("initialize".equals(method)) {
                    return json("{\"userAgent\":\"codex-test\"}");
                }
                if ("thread/start".equals(method)) {
                    return json("{\"thread\":{\"id\":\"thread-123\"}}");
                }
                if ("turn/start".equals(method)) {
                    return json("{\"turn\":{\"id\":\"turn-3\",\"status\":\"failed\",\"error\":{\"message\":\"turn failed\"},\"items\":[]}}");
                }
                throw new IllegalStateException("Unexpected method " + method);
            }),
            callbackClient,
            Path.of(".").toAbsolutePath().normalize()
        );

        assertThrows(IllegalStateException.class, () -> client.dispatch(request("")));
        assertTrue(callbackClient.accepted.isEmpty());
        assertEquals(1, callbackClient.outcomes.size());
        assertEquals("failed", callbackClient.outcomes.getFirst().status());
    }

    @Test
    void dispatchReportsFallbackOutcomeWhenTurnCompletesWithoutDurableCallback() throws Exception {
        final CountDownLatch outcomeReported = new CountDownLatch(1);
        final CapturingCallbackClient callbackClient = new CapturingCallbackClient(outcomeReported);
        final CompletableFuture<CodexTurnOutcome> turnOutcome = new CompletableFuture<>();
        final CodexAppServerConversationClient client = new CodexAppServerConversationClient(
            enabledConfig(),
            OBJECT_MAPPER,
            new FakeSessionFactory(
                (method, params) -> {
                    if ("initialize".equals(method)) {
                        return json("{\"userAgent\":\"codex-test\"}");
                    }
                    if ("thread/start".equals(method)) {
                        return json("{\"thread\":{\"id\":\"thread-123\"}}");
                    }
                    if ("turn/start".equals(method)) {
                        return json("{\"turn\":{\"id\":\"turn-9\",\"status\":\"inProgress\",\"error\":null,\"items\":[]}}");
                    }
                    throw new IllegalStateException("Unexpected method " + method);
                },
                turnOutcome
            ),
            callbackClient,
            Path.of(".").toAbsolutePath().normalize()
        );

        final CodexExecutionDispatchResult result = client.dispatch(request(""));

        assertEquals("thread-123", result.codexThreadId());
        turnOutcome.complete(new CodexTurnOutcome("turn-9", "completed", "", "Blocked: bridge tool calls are not available."));
        assertTrue(outcomeReported.await(2, TimeUnit.SECONDS));
        assertEquals(1, callbackClient.outcomes.size());
        assertEquals("failed", callbackClient.outcomes.getFirst().status());
        assertTrue(callbackClient.outcomes.getFirst().note().contains("Blocked: bridge tool calls are not available."));
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
            "Prepare the branch environment.",
            codexThreadId
        );
    }

    private static CodexAppServerConfig disabledConfig() throws Exception {
        final CodexAppServerConfig config = new CodexAppServerConfig();
        set(config, "enabled", false);
        set(config, "url", "");
        set(config, "timeoutSeconds", 10);
        return config;
    }

    private static CodexAppServerConfig enabledConfig() throws Exception {
        return enabledConfig(null, "workspace-write");
    }

    private static CodexAppServerConfig enabledConfig(final Path workspaceRootDir) throws Exception {
        return enabledConfig(workspaceRootDir, "workspace-write");
    }

    private static CodexAppServerConfig enabledConfig(final Path workspaceRootDir, final String sandbox) throws Exception {
        final CodexAppServerConfig config = new CodexAppServerConfig();
        set(config, "enabled", true);
        set(config, "url", "ws://127.0.0.1:12345");
        set(config, "timeoutSeconds", 10);
        set(config, "workspaceRootDir", workspaceRootDir == null ? "" : workspaceRootDir.toString());
        set(config, "sandbox", sandbox);
        return config;
    }

    private static JsonNode json(final String value) {
        try {
            return OBJECT_MAPPER.readTree(value);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void set(final Object target, final String fieldName, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private record FakeSessionFactory(
        BiFunction<String, JsonNode, JsonNode> handler,
        CompletableFuture<CodexTurnOutcome> turnOutcome
    ) implements CodexAppServerSessionFactory {
        private FakeSessionFactory(final BiFunction<String, JsonNode, JsonNode> handler) {
            this(handler, new CompletableFuture<>());
        }

        @Override
        public CodexAppServerSession open(final URI uri, final Duration timeout) {
            return new FakeSession(handler, turnOutcome);
        }
    }

    private static final class FakeSession implements CodexAppServerSession {
        private final BiFunction<String, JsonNode, JsonNode> handler;
        private final CompletableFuture<CodexTurnOutcome> turnOutcome;

        private FakeSession(final BiFunction<String, JsonNode, JsonNode> handler, final CompletableFuture<CodexTurnOutcome> turnOutcome) {
            this.handler = handler;
            this.turnOutcome = turnOutcome;
        }

        @Override
        public JsonNode request(final String method, final Object params, final Duration timeout) {
            return handler.apply(method, OBJECT_MAPPER.valueToTree(params));
        }

        @Override
        public void notify(final String method, final Object params) {
            // Notifications are irrelevant for these tests.
        }

        @Override
        public CompletableFuture<CodexTurnOutcome> turnOutcome(final String turnId) {
            return turnOutcome;
        }

        @Override
        public void close() {
            // Nothing to close for the fake session.
        }
    }

    private static final class CapturingCallbackClient implements net.mudpot.constructraos.codexbridge.callback.CodexExecutionCallbackClient {
        private final List<String> accepted = new ArrayList<>();
        private final List<Outcome> outcomes = new ArrayList<>();
        private final CountDownLatch outcomeReported;

        private CapturingCallbackClient() {
            this(null);
        }

        private CapturingCallbackClient(final CountDownLatch outcomeReported) {
            this.outcomeReported = outcomeReported;
        }

        @Override
        public void reportAccepted(final CodexExecutionDispatchRequest request, final String codexThreadId, final String note) {
            accepted.add(codexThreadId + "|" + note);
        }

        @Override
        public void reportSreEnvironmentOutcome(
            final CodexExecutionDispatchRequest request,
            final String environmentName,
            final String status,
            final String note
        ) {
            outcomes.add(new Outcome(environmentName, status, note));
            if (outcomeReported != null) {
                outcomeReported.countDown();
            }
        }
    }

    private record Outcome(String environmentName, String status, String note) {
    }
}
