package net.mudpot.constructraos.codexbridge.callback;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.mudpot.constructraos.codexbridge.config.ConstructraApiConfig;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConstructraApiExecutionCallbackClientTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void reportAcceptedPostsToTaskWorkflowAcceptedEndpoint() throws Exception {
        final CapturedRequest capturedRequest = new CapturedRequest();
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/projects/constructraos/tasks/T-0001/codex-executions/accepted", exchange -> handleAccepted(exchange, capturedRequest));
        server.start();
        try {
            final ConstructraApiExecutionCallbackClient client = new ConstructraApiExecutionCallbackClient(
                enabledConfig(server.getAddress().getPort()),
                OBJECT_MAPPER
            );

            client.reportAccepted(request(), "codex-thread-123", "Bridge accepted execution.");

            assertEquals("POST", capturedRequest.method);
            assertNotNull(capturedRequest.body);
            assertEquals("T-0001-exec-1", capturedRequest.body.path("executionRequestId").asText());
            assertEquals("codex-thread-123", capturedRequest.body.path("codexThreadId").asText());
            assertEquals("SRE", capturedRequest.body.path("specialistRole").asText());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void reportSreEnvironmentOutcomePostsToTaskWorkflowOutcomeEndpoint() throws Exception {
        final CapturedRequest capturedRequest = new CapturedRequest();
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/projects/constructraos/tasks/T-0001/sre-environment-outcomes", exchange -> handleAccepted(exchange, capturedRequest));
        server.start();
        try {
            final ConstructraApiExecutionCallbackClient client = new ConstructraApiExecutionCallbackClient(
                enabledConfig(server.getAddress().getPort()),
                OBJECT_MAPPER
            );

            client.reportSreEnvironmentOutcome(request(), "", "failed", "Bridge observed a completed turn without a durable callback.");

            assertEquals("POST", capturedRequest.method);
            assertNotNull(capturedRequest.body);
            assertEquals("project/constructraos/integration", capturedRequest.body.path("branchName").asText());
            assertEquals("failed", capturedRequest.body.path("status").asText());
            assertEquals("Bridge observed a completed turn without a durable callback.", capturedRequest.body.path("note").asText());
        } finally {
            server.stop(0);
        }
    }

    private static void handleAccepted(final HttpExchange exchange, final CapturedRequest capturedRequest) throws IOException {
        capturedRequest.method = exchange.getRequestMethod();
        try (InputStream bodyStream = exchange.getRequestBody()) {
            capturedRequest.body = OBJECT_MAPPER.readTree(new String(bodyStream.readAllBytes(), StandardCharsets.UTF_8));
        }
        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }

    private static ConstructraApiConfig enabledConfig(final int port) throws Exception {
        final ConstructraApiConfig config = new ConstructraApiConfig();
        set(config, "enabled", true);
        set(config, "url", "http://127.0.0.1:" + port);
        set(config, "timeoutSeconds", 10);
        return config;
    }

    private static CodexExecutionDispatchRequest request() {
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
            ""
        );
    }

    private static void set(final Object target, final String fieldName, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class CapturedRequest {
        private String method;
        private JsonNode body;
    }
}
