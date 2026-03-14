package net.mudpot.constructraos.commons.orchestration.codex.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public record CodexExecutionResult(
    String status,
    String summary,
    @JsonProperty("recommended_next_agent")
    String recommendedNextAgent
) {
    private static final Set<String> VALID_STATUSES = Set.of("completed", "blocked", "failed");

    public CodexExecutionResult {
        status = normalizeStatus(status);
        summary = normalizeSummary(summary);
        recommendedNextAgent = normalizeNextAgent(recommendedNextAgent);
    }

    private static String normalizeStatus(final String value) {
        final String normalized = sanitize(value).toLowerCase();
        if (!VALID_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported codex execution status: " + value);
        }
        return normalized;
    }

    private static String normalizeSummary(final String value) {
        final String normalized = sanitize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Codex execution summary is required.");
        }
        return normalized;
    }

    private static String normalizeNextAgent(final String value) {
        final String normalized = sanitize(value);
        return normalized.isBlank() ? "none" : normalized;
    }

    private static String sanitize(final String value) {
        return value == null ? "" : value.trim();
    }
}
