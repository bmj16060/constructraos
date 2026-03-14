package net.mudpot.constructraos.apiservice.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import net.mudpot.constructraos.apiservice.tasks.TaskActorContext;
import net.mudpot.constructraos.apiservice.tasks.TaskStartRequest;
import net.mudpot.constructraos.apiservice.tasks.TaskStartResponse;
import net.mudpot.constructraos.apiservice.tasks.TaskStatusResponse;
import net.mudpot.constructraos.apiservice.tasks.TaskSurfaceService;
import net.mudpot.constructraos.apiservice.tasks.TaskTranscriptResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(transactional = false)
class TaskMcpResourceTest {
    @Inject
    EmbeddedServer embeddedServer;

    @Inject
    ObjectMapper objectMapper;

    @MockBean(TaskSurfaceService.class)
    TaskSurfaceService taskSurfaceService() {
        return new StubTaskSurfaceService();
    }

    @Test
    void exposesTaskResourcesAndTemplates() throws Exception {
        initializeSession();

        final Map<String, Object> resourcesList = rpcRequest("2", McpSchema.METHOD_RESOURCES_LIST, Map.of());
        @SuppressWarnings("unchecked")
        final Map<String, Object> resourcesResult = (Map<String, Object>) resourcesList.get("result");
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> resources = (List<Map<String, Object>>) resourcesResult.get("resources");

        assertEquals(List.of("constructraos://tasks/recent"), resources.stream().map(resource -> String.valueOf(resource.get("uri"))).toList());

        final Map<String, Object> templatesList = rpcRequest("3", McpSchema.METHOD_RESOURCES_TEMPLATES_LIST, Map.of());
        @SuppressWarnings("unchecked")
        final Map<String, Object> templatesResult = (Map<String, Object>) templatesList.get("result");
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> templates = (List<Map<String, Object>>) templatesResult.get("resourceTemplates");

        assertEquals(
            List.of("constructraos://tasks/{workflowId}/status", "constructraos://tasks/{workflowId}/transcript"),
            templates.stream().map(template -> String.valueOf(template.get("uriTemplate"))).sorted().toList()
        );
    }

    @Test
    void canReadRecentTasksStatusAndTranscriptResources() throws Exception {
        initializeSession();

        final Map<String, Object> recentResponse = rpcRequest(
            "4",
            McpSchema.METHOD_RESOURCES_READ,
            Map.of("uri", "constructraos://tasks/recent")
        );
        @SuppressWarnings("unchecked")
        final Map<String, Object> recentResult = (Map<String, Object>) recentResponse.get("result");
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> recentContents = (List<Map<String, Object>>) recentResult.get("contents");
        final String recentJson = String.valueOf(recentContents.getFirst().get("text"));
        final List<Map<String, Object>> recentTasks = objectMapper.readValue(recentJson, new TypeReference<>() { });

        assertTrue(recentTasks.stream().anyMatch(task -> "completed".equals(task.get("status"))));

        final String workflowId = String.valueOf(recentTasks.getFirst().get("workflow_id"));

        final Map<String, Object> statusResponse = rpcRequest(
            "5",
            McpSchema.METHOD_RESOURCES_READ,
            Map.of("uri", "constructraos://tasks/" + workflowId + "/status")
        );
        @SuppressWarnings("unchecked")
        final Map<String, Object> statusResult = (Map<String, Object>) statusResponse.get("result");
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> statusContents = (List<Map<String, Object>>) statusResult.get("contents");
        final Map<String, Object> statusPayload = objectMapper.readValue(String.valueOf(statusContents.getFirst().get("text")), new TypeReference<>() { });

        assertEquals(workflowId, statusPayload.get("workflow_id"));

        final Map<String, Object> transcriptResponse = rpcRequest(
            "6",
            McpSchema.METHOD_RESOURCES_READ,
            Map.of("uri", "constructraos://tasks/" + workflowId + "/transcript")
        );
        @SuppressWarnings("unchecked")
        final Map<String, Object> transcriptResult = (Map<String, Object>) transcriptResponse.get("result");
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> transcriptContents = (List<Map<String, Object>>) transcriptResult.get("contents");
        final Map<String, Object> transcriptPayload = objectMapper.readValue(String.valueOf(transcriptContents.getFirst().get("text")), new TypeReference<>() { });

        assertEquals(workflowId, transcriptPayload.get("workflow_id"));
        assertTrue(transcriptPayload.containsKey("transcript_payload"));
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

    private static final class StubTaskSurfaceService extends TaskSurfaceService {
        private static final String WORKFLOW_ID = "wf-resource-1";

        private StubTaskSurfaceService() {
            super(null, null, null, "");
        }

        @Override
        public List<TaskStatusResponse> listTasks(final String workingDirectory, final int limit, final TaskActorContext actor) {
            return List.of(status());
        }

        @Override
        public TaskStatusResponse getTaskStatus(final String workflowId, final TaskActorContext actor) {
            return status();
        }

        @Override
        public TaskTranscriptResponse getTaskTranscript(final String workflowId, final TaskActorContext actor) {
            return new TaskTranscriptResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                WORKFLOW_ID,
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                1,
                "completed",
                "planner",
                "codex-cli-jsonl",
                "thread-1",
                List.of("{\"type\":\"thread.started\"}"),
                Instant.parse("2026-03-14T00:00:00Z"),
                Instant.parse("2026-03-14T00:01:00Z")
            );
        }

        @Override
        public TaskStartResponse startTask(final TaskStartRequest request, final TaskActorContext actor) {
            return new TaskStartResponse("CodexExecutionWorkflow", WORKFLOW_ID, "codex-execution-task-queue", "run-1");
        }

        private static TaskStatusResponse status() {
            return new TaskStatusResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                WORKFLOW_ID,
                "workspace",
                "/workspace",
                "completed",
                "Resource verification task.",
                "planner",
                "mcp",
                "mcp-tool",
                1,
                "completed",
                "planner",
                "Resource verification passed.",
                "none",
                "thread-1",
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Map.of("status", "completed"),
                Instant.parse("2026-03-14T00:00:00Z"),
                Instant.parse("2026-03-14T00:01:00Z")
            );
        }
    }
}
