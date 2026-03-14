package net.mudpot.constructraos.apiservice.controllers;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import net.mudpot.constructraos.apiservice.session.AnonymousSession;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionRequest;

import java.nio.file.Paths;
import java.util.Map;

public final class CodexExecutionWorkflowSupport {
    private static final String DEFAULT_AGENT_NAME = "planner";

    private CodexExecutionWorkflowSupport() {
    }

    public static CodexExecutionRequest normalizedRequest(final CodexExecutionRequest request) {
        if (request == null || sanitize(request.prompt()).isBlank()) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Prompt is required.");
        }
        return new CodexExecutionRequest(
            sanitize(request.prompt()),
            normalizedWorkingDirectory(request.workingDirectory()),
            normalizedAgentName(request.agentName()),
            sanitize(request.workflowId())
        );
    }

    public static Map<String, Object> actorInput(final AnonymousSession session) {
        return Map.of(
            "kind", session.actorKind(),
            "session_id", session.sessionId()
        );
    }

    private static String normalizedWorkingDirectory(final String workingDirectory) {
        final String normalized = sanitize(workingDirectory);
        return normalized.isBlank()
            ? Paths.get("").toAbsolutePath().normalize().toString()
            : normalized;
    }

    private static String normalizedAgentName(final String agentName) {
        final String normalized = sanitize(agentName);
        return normalized.isBlank() ? DEFAULT_AGENT_NAME : normalized;
    }

    private static String sanitize(final String value) {
        return value == null ? "" : value.trim();
    }
}
