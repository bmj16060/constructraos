package net.mudpot.constructraos.persistence.runtimecoordination;

import java.util.Map;

public record RuntimeExecutionUpdate(
    String providerSessionId,
    String checkpointKind,
    Map<String, Object> checkpointPayload
) {
    public RuntimeExecutionUpdate {
        checkpointPayload = checkpointPayload == null ? Map.of() : Map.copyOf(checkpointPayload);
    }
}
