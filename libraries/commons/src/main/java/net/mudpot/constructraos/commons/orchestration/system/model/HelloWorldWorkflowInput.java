package net.mudpot.constructraos.commons.orchestration.system.model;

public record HelloWorldWorkflowInput(
    String name,
    String useCase,
    String actorKind,
    String sessionId
) {
}
