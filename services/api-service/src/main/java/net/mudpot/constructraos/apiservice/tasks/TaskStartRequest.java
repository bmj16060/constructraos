package net.mudpot.constructraos.apiservice.tasks;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TaskStartRequest(
    String goal,
    @JsonProperty("working_directory")
    String workingDirectory,
    @JsonProperty("agent_name")
    String agentName,
    @JsonProperty("workflow_id")
    String workflowId
) {
}
