package net.mudpot.constructraos.commons.orchestration.codex.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CodexExecutionRequest(
    String prompt,
    @JsonProperty("working_directory")
    String workingDirectory,
    @JsonProperty("agent_name")
    String agentName,
    @JsonProperty("workflow_id")
    String workflowId
) {
}
