package net.mudpot.constructraos.commons.policy.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;
import net.mudpot.constructraos.commons.policy.PolicyEvaluator;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class PolicyServiceClient implements PolicyEvaluator {
    private static final int MAX_ATTEMPTS = 3;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;
    private final String policyServiceUrl;
    private final boolean enforce;

    public PolicyServiceClient(final OpenTelemetry openTelemetry, final String policyServiceUrl, final boolean enforce) {
        this(HttpClient.newBuilder().build(), new ObjectMapper(), openTelemetry, policyServiceUrl, enforce);
    }

    PolicyServiceClient(
        final HttpClient httpClient,
        final ObjectMapper objectMapper,
        final OpenTelemetry openTelemetry,
        final String policyServiceUrl,
        final boolean enforce
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.openTelemetry = openTelemetry;
        this.tracer = openTelemetry.getTracer("constructraos.policy-client");
        this.policyServiceUrl = policyServiceUrl;
        this.enforce = enforce;
    }

    @Override
    public PolicyEvaluationResult evaluate(final PolicyEvaluationRequest request) {
        final Span span = tracer.spanBuilder("policy-service evaluate")
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute("policy.service.url", policyServiceUrl)
            .setAttribute("policy.action", request.action() == null ? "" : request.action())
            .setAttribute("policy.enforce", enforce)
            .setAttribute("http.method", "POST")
            .setAttribute("http.url", policyServiceUrl + "/v1/policy/evaluate")
            .startSpan();
        try (Scope ignored = span.makeCurrent()) {
            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                span.addEvent("policy-service attempt", io.opentelemetry.api.common.Attributes.of(
                    io.opentelemetry.api.common.AttributeKey.longKey("attempt"),
                    (long) attempt
                ));
                try {
                    final HttpRequest httpRequest = buildRequest(request);
                    final HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                    span.setAttribute("http.status_code", response.statusCode());
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return objectMapper.readValue(response.body(), PolicyEvaluationResult.class);
                    }
                    if (response.statusCode() < 500 || attempt == MAX_ATTEMPTS) {
                        span.setStatus(StatusCode.ERROR, "policy-service-http-" + response.statusCode());
                        return enforcementAwareFailure("policy-service-http-" + response.statusCode());
                    }
                } catch (final Exception exception) {
                    span.recordException(exception);
                    if (attempt == MAX_ATTEMPTS) {
                        span.setStatus(StatusCode.ERROR, "policy-service-error");
                        return enforcementAwareFailure("policy-service-error");
                    }
                }
            }
            span.setStatus(StatusCode.ERROR, "policy-service-error");
            return enforcementAwareFailure("policy-service-error");
        } finally {
            span.end();
        }
    }

    private HttpRequest buildRequest(final PolicyEvaluationRequest request) throws Exception {
        final Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        openTelemetry.getPropagators().getTextMapPropagator().inject(
            io.opentelemetry.context.Context.current(),
            headers,
            Map::put
        );

        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(policyServiceUrl + "/v1/policy/evaluate"))
            .timeout(Duration.ofSeconds(5))
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)));
        headers.forEach(requestBuilder::header);
        return requestBuilder.build();
    }

    private PolicyEvaluationResult enforcementAwareFailure(final String reason) {
        if (!enforce) {
            return new PolicyEvaluationResult(true, reason + "-fail-open", "policy-service");
        }
        return new PolicyEvaluationResult(false, reason, "policy-service");
    }
}
