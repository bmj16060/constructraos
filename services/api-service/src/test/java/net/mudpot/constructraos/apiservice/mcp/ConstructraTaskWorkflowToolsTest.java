package net.mudpot.constructraos.apiservice.mcp;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import net.mudpot.constructraos.apiservice.workflow.TaskWorkflowOperationsService;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowSignalResponse;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowState;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class ConstructraTaskWorkflowToolsTest implements TestPropertyProvider {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    EmbeddedServer embeddedServer;

    @Inject
    TaskWorkflowOperationsService taskWorkflowOperationsService;

    @Test
    void toolsListIncludesExplicitTaskWorkflowTools() {
        final String response = post(
            Map.of(
                "jsonrpc", "2.0",
                "id", "1",
                "method", "tools/list",
                "params", Map.of()
            )
        );

        assertTrue(response.contains("constructra_get_task_workflow_state"));
        assertTrue(response.contains("constructra_report_sre_environment_outcome"));
        assertTrue(response.contains("constructra_report_codex_execution_accepted"));
    }

    @Test
    void callToolInvokesWorkflowOperationsService() {
        final StubTaskWorkflowOperationsService service = (StubTaskWorkflowOperationsService) taskWorkflowOperationsService;

        final String response = post(
            Map.of(
                "jsonrpc", "2.0",
                "id", "2",
                "method", "tools/call",
                "params", Map.of(
                    "name", "constructra_report_sre_environment_outcome",
                    "arguments", Map.of(
                        "projectId", "constructraos",
                        "taskId", "T-0001",
                        "branchName", "project/constructraos/integration",
                        "environmentName", "branch-env-01",
                        "status", "ready",
                        "note", "Environment is healthy for QA."
                    )
                )
            )
        );

        assertEquals("constructraos", service.projectId);
        assertEquals("T-0001", service.taskId);
        assertEquals("project/constructraos/integration", service.branchName);
        assertEquals("branch-env-01", service.environmentName);
        assertEquals("ready", service.environmentStatus);
        assertEquals(TaskWorkflowOperationsService.MCP_ACTOR_KIND, service.actorKind);
        assertEquals(TaskWorkflowOperationsService.MCP_SESSION_ID, service.sessionId);
        assertTrue(response.contains("reportSreEnvironmentOutcome"));
    }

    private String post(final Map<String, Object> body) {
        try {
            final URL url = new URL("http://127.0.0.1:" + embeddedServer.getPort() + "/mcp");
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            final byte[] payload = OBJECT_MAPPER.writeValueAsBytes(body);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(payload);
            }
            final int status = connection.getResponseCode();
            final String responseBody;
            try (java.io.InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream()) {
                responseBody = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("MCP endpoint returned status " + status + ": " + responseBody);
            }
            return responseBody;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @MockBean(TaskWorkflowOperationsService.class)
    @Replaces(TaskWorkflowOperationsService.class)
    StubTaskWorkflowOperationsService taskWorkflowOperationsService() {
        return new StubTaskWorkflowOperationsService();
    }

    @Override
    public Map<String, String> getProperties() {
        return Map.of(
            "datasources.default.db-type", "h2",
            "datasources.default.dialect", "H2",
            "datasources.default.driver-class-name", "org.h2.Driver",
            "datasources.default.url", "jdbc:h2:mem:constructraos-mcp;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            "datasources.default.username", "sa",
            "datasources.default.password", "",
            "flyway.datasources.default.enabled", "false",
            "policy.service-enforce", "false"
        );
    }

    static class StubTaskWorkflowOperationsService extends TaskWorkflowOperationsService {
        private String projectId;
        private String taskId;
        private String branchName;
        private String environmentName;
        private String environmentStatus;
        private String actorKind;
        private String sessionId;

        StubTaskWorkflowOperationsService() {
            super(null);
        }

        @Override
        public TaskWorkflowState currentState(final String projectId, final String taskId) {
            return new TaskWorkflowState(projectId, taskId, "OPEN", "in_progress", "SRE", "project/constructraos/integration", "requested", "planned integration environment", "team-t-0001", "T-0001-exec-1", "codex-thread-123", "E-0001", "qa_requested", 1);
        }

        @Override
        public TaskWorkflowSignalResponse reportSreEnvironmentOutcome(
            final String projectId,
            final String taskId,
            final String branchName,
            final String environmentName,
            final String status,
            final String note,
            final String actorKind,
            final String sessionId
        ) {
            this.projectId = projectId;
            this.taskId = taskId;
            this.branchName = branchName;
            this.environmentName = environmentName;
            this.environmentStatus = status;
            this.actorKind = actorKind;
            this.sessionId = sessionId;
            return new TaskWorkflowSignalResponse("TaskCoordinationWorkflow", "wf-task", "task-coordination-task-queue", "", "reportSreEnvironmentOutcome");
        }

        @Override
        public TaskWorkflowSignalResponse reportCodexExecutionAccepted(
            final String projectId,
            final String taskId,
            final String executionRequestId,
            final String codexThreadId,
            final String specialistRole,
            final String note
        ) {
            return new TaskWorkflowSignalResponse("TaskCoordinationWorkflow", "wf-task", "task-coordination-task-queue", "", "reportCodexExecutionAccepted");
        }

        @Override
        public TaskWorkflowSignalResponse requestQa(
            final String projectId,
            final String taskId,
            final String branchName,
            final String note,
            final String actorKind,
            final String sessionId
        ) {
            return new TaskWorkflowSignalResponse("TaskCoordinationWorkflow", "wf-task", "task-coordination-task-queue", "", "requestQa");
        }
    }
}
