package net.mudpot.constructraos.commons.orchestration.codex.execution;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionActivityInput;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionOutcome;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodexRuntimeHttpExecutionAdapterTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void executePostsRenderedRequestAndParsesStructuredResult() throws Exception {
        final AtomicReference<ExecutionRequest> capturedRequest = new AtomicReference<>();
        try (TestRuntimeServer runtimeServer = new TestRuntimeServer(exchange -> {
            capturedRequest.set(readRequest(exchange));
            writeJson(
                exchange,
                200,
                new ExecutionResponse(
                    0,
                    List.of(
                        "{\"type\":\"item.completed\",\"item\":{\"type\":\"agent_message\",\"text\":\"{\\\"status\\\":\\\"completed\\\",\\\"summary\\\":\\\"Containerized runtime responded.\\\",\\\"recommended_next_agent\\\":\\\"reviewer\\\"}\"}}"
                    ),
                    ""
                )
            );
        })) {
            final CodexRuntimeHttpExecutionAdapter adapter = new CodexRuntimeHttpExecutionAdapter(
                new CodexRuntimeHttpSettings(runtimeServer.baseUrl(), 5, "/workspace")
            );

            final CodexExecutionOutcome result = adapter.execute(
                new CodexExecutionActivityInput("wf-1", "Summarize the next step.", "", "planner", "anonymous", "anon-session-1")
            );

            assertEquals("completed", result.result().status());
            assertEquals("Containerized runtime responded.", result.result().summary());
            assertEquals("reviewer", result.result().recommendedNextAgent());
            assertEquals("", result.sessionId());
            assertEquals(1, result.transcriptLines().size());
            assertEquals("/workspace", capturedRequest.get().workingDirectory());
            assertTrue(capturedRequest.get().prompt().contains("Summarize the next step."));
            assertTrue(capturedRequest.get().outputSchema().contains("\"recommended_next_agent\""));
        }
    }

    @Test
    void executeSurfacesRuntimeErrors() throws Exception {
        try (TestRuntimeServer runtimeServer = new TestRuntimeServer(exchange -> writeJson(
            exchange,
            500,
            new ExecutionResponse(-1, List.of(), "Codex runtime is not configured.")
        ))) {
            final CodexRuntimeHttpExecutionAdapter adapter = new CodexRuntimeHttpExecutionAdapter(
                new CodexRuntimeHttpSettings(runtimeServer.baseUrl(), 5, "/workspace")
            );

            final CodexExecutionException exception = assertThrows(
                CodexExecutionException.class,
                () -> adapter.execute(new CodexExecutionActivityInput("wf-1", "Summarize the next step.", "", "planner", "anonymous", "anon-session-1"))
            );

            assertEquals("Codex runtime is not configured.", exception.getMessage());
        }
    }

    private ExecutionRequest readRequest(final HttpExchange exchange) throws IOException {
        return objectMapper.readValue(exchange.getRequestBody(), ExecutionRequest.class);
    }

    private void writeJson(final HttpExchange exchange, final int statusCode, final Object body) throws IOException {
        final byte[] payload = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, payload.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(payload);
        } finally {
            exchange.close();
        }
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private static final class TestRuntimeServer implements AutoCloseable {
        private final HttpServer server;

        private TestRuntimeServer(final ExchangeHandler handler) throws IOException {
            this.server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/executions", exchange -> handler.handle(exchange));
            server.start();
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    private record ExecutionRequest(
        String prompt,
        @JsonProperty("working_directory")
        String workingDirectory,
        @JsonProperty("output_schema")
        String outputSchema,
        @JsonProperty("timeout_seconds")
        long timeoutSeconds
    ) {
    }

    private record ExecutionResponse(
        @JsonProperty("exit_code")
        int exitCode,
        List<String> lines,
        String error
    ) {
    }
}
