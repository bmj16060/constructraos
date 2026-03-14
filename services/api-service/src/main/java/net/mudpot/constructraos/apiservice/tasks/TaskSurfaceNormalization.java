package net.mudpot.constructraos.apiservice.tasks;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;

import java.nio.file.Path;
import java.util.Map;

public final class TaskSurfaceNormalization {
    private static final String DEFAULT_AGENT_NAME = "planner";

    private TaskSurfaceNormalization() {
    }

    public static TaskStartRequest normalizedRequest(final TaskStartRequest request, final String defaultWorkingDirectory) {
        if (request == null || sanitize(request.goal()).isBlank()) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Goal is required.");
        }
        return new TaskStartRequest(
            sanitize(request.goal()),
            normalizedWorkingDirectory(request.workingDirectory(), defaultWorkingDirectory),
            normalizedAgentName(request.agentName()),
            sanitize(request.workflowId())
        );
    }

    public static String normalizedWorkflowId(final String workflowId) {
        final String normalizedWorkflowId = sanitize(workflowId);
        if (normalizedWorkflowId.isBlank()) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Workflow ID is required.");
        }
        return normalizedWorkflowId;
    }

    public static String normalizedWorkingDirectory(final String workingDirectory, final String defaultWorkingDirectory) {
        final String normalized = sanitize(workingDirectory);
        final String normalizedDefault = sanitize(defaultWorkingDirectory);
        if (normalized.isBlank() && !normalizedDefault.isBlank()) {
            return Path.of(normalizedDefault).toAbsolutePath().normalize().toString();
        }
        return normalized.isBlank()
            ? Path.of("").toAbsolutePath().normalize().toString()
            : Path.of(normalized).toAbsolutePath().normalize().toString();
    }

    public static int normalizedLimit(final int limit) {
        return Math.max(1, Math.min(limit, 50));
    }

    public static Map<String, Object> actorInput(final TaskActorContext actor) {
        return Map.of(
            "kind", sanitize(actor == null ? "" : actor.actorKind()),
            "session_id", sanitize(actor == null ? "" : actor.sessionId())
        );
    }

    private static String normalizedAgentName(final String agentName) {
        final String normalized = sanitize(agentName);
        return normalized.isBlank() ? DEFAULT_AGENT_NAME : normalized;
    }

    public static String sanitize(final String value) {
        return value == null ? "" : value.trim();
    }
}
