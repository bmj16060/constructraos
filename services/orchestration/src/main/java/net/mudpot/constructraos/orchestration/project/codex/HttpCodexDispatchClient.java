package net.mudpot.constructraos.orchestration.project.codex;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchRequest;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchResult;
import net.mudpot.constructraos.orchestration.config.CodexAdapterConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Singleton
public class HttpCodexDispatchClient implements CodexDispatchClient {
    private final CodexAdapterConfig config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public HttpCodexDispatchClient(final CodexAdapterConfig config, final ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(config.timeout())
            .build();
    }

    @Override
    public CodexExecutionDispatchResult dispatch(final CodexExecutionDispatchRequest request) {
        if (!config.enabled() || config.url().isBlank()) {
            return new CodexExecutionDispatchResult(
                request.executionRequestId(),
                "",
                "dispatched",
                "Codex adapter is not configured yet. Request persisted for external consumption."
            );
        }

        try {
            final HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(config.url() + "/dispatch"))
                .timeout(config.timeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                .build();
            final HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Codex adapter returned status " + response.statusCode() + ": " + response.body());
            }
            return objectMapper.readValue(response.body(), CodexExecutionDispatchResult.class);
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Failed dispatching Codex execution request.", exception);
        }
    }
}
