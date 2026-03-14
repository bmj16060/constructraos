package net.mudpot.constructraos.persistence.tasks;

import java.util.UUID;

public record TaskExecutionContext(
    UUID projectId,
    UUID taskId,
    UUID taskStepId,
    UUID agentSessionId,
    UUID runtimeExecutionId
) {
}
