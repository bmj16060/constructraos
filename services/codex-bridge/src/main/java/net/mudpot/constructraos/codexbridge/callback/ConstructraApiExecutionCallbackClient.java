package net.mudpot.constructraos.codexbridge.callback;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.codexbridge.config.ConstructraApiConfig;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Singleton
public class ConstructraApiExecutionCallbackClient implements CodexExecutionCallbackClient {
    private final ConstructraApiConfig config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ConstructraApiExecutionCallbackClient(final ConstructraApiConfig config, final ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(config.timeout())
            .build();
    }

    @Override
    public void reportAccepted(final CodexExecutionDispatchRequest request, final String codexThreadId, final String note) {
        if (!config.enabled() || config.url().isBlank()) {
            return;
        }
        if (!"reportCodexExecutionAccepted".equals(normalize(request.callbackSignal()))) {
            return;
        }
        final String projectId = normalize(request.projectId());
        final String taskId = normalize(request.taskId());
        if (projectId.isBlank() || taskId.isBlank()) {
            return;
        }

        final String baseUrl = trimTrailingSlash(config.url());
        final URI uri = URI.create(baseUrl + "/api/projects/" + projectId + "/tasks/" + taskId + "/codex-executions/accepted");
        final Map<String, Object> payload = Map.of(
            "executionRequestId", normalize(request.executionRequestId()),
            "codexThreadId", normalize(codexThreadId),
            "specialistRole", normalize(request.specialistRole()),
            "note", normalize(note)
        );

        try {
            final HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                .timeout(config.timeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
            final HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Constructra API accepted callback returned status " + response.statusCode() + ": " + response.body());
            }
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Failed reporting accepted Codex execution back to Constructra API.", exception);
        }
    }

    private static String trimTrailingSlash(final String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String normalize(final String value) {
        return value == null ? "" : value.trim();
    }
}
