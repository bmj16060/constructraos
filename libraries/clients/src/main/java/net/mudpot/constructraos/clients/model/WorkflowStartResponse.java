package net.mudpot.constructraos.clients.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record WorkflowStartResponse(
    String workflow,
    @JsonProperty("workflow_id")
    String workflowId,
    @JsonProperty("task_queue")
    String taskQueue,
    @JsonProperty("run_id")
    String runId
) {
    public Map<String, String> toMap() {
        return Map.of(
            "workflow", workflow,
            "workflow_id", workflowId,
            "task_queue", taskQueue,
            "run_id", runId
        );
    }
}
