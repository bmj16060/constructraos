package net.mudpot.constructraos.commons.orchestration.system.model;

public record HelloWorldRequest(
    String name,
    String useCase,
    String workflowId
) {
}
