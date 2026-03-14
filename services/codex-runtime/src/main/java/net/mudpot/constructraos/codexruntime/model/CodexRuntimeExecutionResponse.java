package net.mudpot.constructraos.codexruntime.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CodexRuntimeExecutionResponse(
    @JsonProperty("exit_code")
    int exitCode,
    List<String> lines,
    String error
) {
    public CodexRuntimeExecutionResponse {
        lines = lines == null ? List.of() : List.copyOf(lines);
        error = error == null ? "" : error;
    }
}
