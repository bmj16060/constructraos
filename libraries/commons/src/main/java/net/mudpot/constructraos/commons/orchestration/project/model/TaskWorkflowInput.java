package net.mudpot.constructraos.commons.orchestration.project.model;

public record TaskWorkflowInput(
    String projectId,
    String taskId,
    String actorKind,
    String sessionId
) {
}
