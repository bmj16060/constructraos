package net.mudpot.constructraos.commons.orchestration.system.model;

import java.time.Instant;
import java.util.UUID;

public record HelloHistoryEntry(
    UUID id,
    String workflowId,
    String name,
    String useCase,
    String greeting,
    String provider,
    String model,
    boolean cacheHit,
    String promptTemplate,
    Instant createdAt
) {
}
