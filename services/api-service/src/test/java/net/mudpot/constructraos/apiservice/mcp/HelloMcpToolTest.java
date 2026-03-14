package net.mudpot.constructraos.apiservice.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@MicronautTest(transactional = false)
class HelloMcpToolTest {
    @Inject
    EmbeddedServer embeddedServer;

    @Inject
    ObjectMapper objectMapper;

    @Test
    void exposesOnlyHelloTool() throws Exception {
        initializeSession();

        final Map<String, Object> toolsList = rpcRequest("2", McpSchema.METHOD_TOOLS_LIST, Map.of());
        @SuppressWarnings("unchecked")
        final Map<String, Object> result = (Map<String, Object>) toolsList.get("result");
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get("tools");

        assertEquals(List.of("hello", "task_list", "task_start", "task_status"), tools.stream().map(tool -> String.valueOf(tool.get("name"))).sorted().toList());
    }

    @Test
    void helloToolReturnsGreeting() throws Exception {
        initializeSession();

        final Map<String, Object> response = rpcRequest(
            "3",
            McpSchema.METHOD_TOOLS_CALL,
            Map.of(
                "name", "hello",
                "arguments", Map.of()
            )
        );
        @SuppressWarnings("unchecked")
        final Map<String, Object> result = (Map<String, Object>) response.get("result");
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
        final Object first = content.getFirst().get("text");

        assertFalse(Boolean.TRUE.equals(result.get("isError")));
        assertInstanceOf(String.class, first);
        assertEquals("Hello from ConstructraOS API.", first);
    }

    private void initializeSession() throws Exception {
        rpcRequest(
            "1",
            McpSchema.METHOD_INITIALIZE,
            Map.of(
                "protocolVersion", McpSchema.LATEST_PROTOCOL_VERSION,
                "capabilities", Map.of(),
                "clientInfo", Map.of(
                    "name", "api-service-test",
                    "version", "1.0.0"
                )
            )
        );

        try (HttpClient httpClient = HttpClient.create(embeddedServer.getURL())) {
            httpClient.toBlocking().exchange(
                HttpRequest.POST(
                    "/mcp",
                    Map.of(
                        "jsonrpc", McpSchema.JSONRPC_VERSION,
                        "method", McpSchema.METHOD_NOTIFICATION_INITIALIZED
                    )
                ).contentType(MediaType.APPLICATION_JSON_TYPE)
            );
        }
    }

    private Map<String, Object> rpcRequest(final String id, final String method, final Map<String, Object> params) throws Exception {
        try (HttpClient httpClient = HttpClient.create(embeddedServer.getURL())) {
            final String responseBody = httpClient.toBlocking().retrieve(
                HttpRequest.POST(
                    "/mcp",
                    Map.of(
                        "jsonrpc", McpSchema.JSONRPC_VERSION,
                        "id", id,
                        "method", method,
                        "params", params
                    )
                ).contentType(MediaType.APPLICATION_JSON_TYPE)
            );
            return objectMapper.readValue(responseBody, new TypeReference<>() { });
        }
    }
}
