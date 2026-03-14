package net.mudpot.constructraos.persistence.runtimecoordination;

import java.util.UUID;

public record RuntimeExecutionStartRequest(
    String workflowId,
    UUID taskId,
    UUID taskStepId,
    String executionMode,
    String checkpointKind
) {
}
