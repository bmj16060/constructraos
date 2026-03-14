package net.mudpot.constructraos.codexruntime.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CodexRuntimeExecutionRequest(
    String prompt,
    @JsonProperty("working_directory")
    String workingDirectory,
    @JsonProperty("output_schema")
    String outputSchema,
    @JsonProperty("timeout_seconds")
    Long timeoutSeconds
) {
}
