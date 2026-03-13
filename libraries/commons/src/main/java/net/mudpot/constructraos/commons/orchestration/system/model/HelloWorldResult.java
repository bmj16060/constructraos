package net.mudpot.constructraos.commons.orchestration.system.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record HelloWorldResult(
    String workflowId,
    String promptTemplate,
    String greeting,
    String provider,
    String model,
    Map<String, Object> usage,
    Map<String, Object> cache,
    Instant createdAt
) {
    public HelloWorldResult {
        usage = usage == null ? new LinkedHashMap<>() : new LinkedHashMap<>(usage);
        cache = cache == null ? new LinkedHashMap<>() : new LinkedHashMap<>(cache);
    }
}
