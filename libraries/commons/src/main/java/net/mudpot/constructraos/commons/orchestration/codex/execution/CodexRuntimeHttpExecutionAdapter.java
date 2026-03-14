package net.mudpot.constructraos.commons.orchestration.codex.execution;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionActivityInput;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionResult;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class CodexRuntimeHttpExecutionAdapter implements CodexExecutionAdapter {
    private final CodexRuntimeHttpSettings settings;
    private final CodexExecutionResourceSupport resourceSupport;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    private final CodexCliJsonOutputParser outputParser = new CodexCliJsonOutputParser();

    public CodexRuntimeHttpExecutionAdapter(final CodexRuntimeHttpSettings settings) {
        this.settings = settings;
        this.resourceSupport = new CodexExecutionResourceSupport(settings.defaultWorkingDirectory());
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(Math.max(1, settings.timeoutSeconds())))
            .build();
    }

    @Override
    public CodexExecutionResult execute(final CodexExecutionActivityInput input) {
        final CodexRuntimeExecutionResponse executionResponse;
        try {
            final String requestJson = objectMapper.writeValueAsString(
                new CodexRuntimeExecutionRequest(
                    resourceSupport.renderPrompt(input),
                    resourceSupport.resolvedWorkingDirectory(input),
                    resourceSupport.schemaJson(),
                    settings.timeoutSeconds()
                )
            );

            final HttpRequest request = HttpRequest.newBuilder()
                .uri(executionsUri())
                .timeout(Duration.ofSeconds(Math.max(1, settings.timeoutSeconds()) + 5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

            // Keep the client single-shot so Temporal activity retry policy remains the retry authority.
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            executionResponse = parseExecutionResponse(response);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Codex runtime request interrupted.", exception);
        } catch (final IOException exception) {
            throw new RuntimeException("Codex runtime request failed.", exception);
        }

        final CodexCliJsonOutputParser.ParsedCodexCliOutput parsed = outputParser.parse(executionResponse.lines());
        if (executionResponse.exitCode() != 0) {
            final String error = !parsed.errorMessage().isBlank()
                ? parsed.errorMessage()
                : sanitize(executionResponse.error()).isBlank()
                    ? "Codex execution exited with code " + executionResponse.exitCode() + "."
                    : sanitize(executionResponse.error());
            throw new IllegalStateException(error);
        }

        if (!parsed.errorMessage().isBlank()) {
            throw new IllegalStateException(parsed.errorMessage());
        }
        return parsed.result();
    }

    private URI executionsUri() {
        final String baseUrl = sanitize(settings.baseUrl());
        if (baseUrl.isBlank()) {
            throw new IllegalStateException("Codex runtime base URL is required.");
        }
        return URI.create(baseUrl.endsWith("/") ? baseUrl + "executions" : baseUrl + "/executions");
    }

    private CodexRuntimeExecutionResponse parseExecutionResponse(final HttpResponse<String> response) throws IOException {
        final CodexRuntimeExecutionResponse parsed = objectMapper.readValue(response.body(), CodexRuntimeExecutionResponse.class);
        if (response.statusCode() >= 400) {
            final String error = sanitize(parsed.error()).isBlank()
                ? "Codex runtime returned HTTP " + response.statusCode() + "."
                : sanitize(parsed.error());
            throw new IllegalStateException(error);
        }
        return parsed;
    }

    private static String sanitize(final String value) {
        return value == null ? "" : value.trim();
    }

    private record CodexRuntimeExecutionRequest(
        String prompt,
        @JsonProperty("working_directory")
        String workingDirectory,
        @JsonProperty("output_schema")
        String outputSchema,
        @JsonProperty("timeout_seconds")
        long timeoutSeconds
    ) {
    }

    private record CodexRuntimeExecutionResponse(
        @JsonProperty("exit_code")
        int exitCode,
        List<String> lines,
        String error
    ) {
        private CodexRuntimeExecutionResponse {
            lines = lines == null ? List.of() : List.copyOf(lines);
            error = error == null ? "" : error;
        }
    }
}
