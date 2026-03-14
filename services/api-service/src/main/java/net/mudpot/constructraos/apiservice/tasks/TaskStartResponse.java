package net.mudpot.constructraos.apiservice.tasks;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.mudpot.constructraos.clients.model.WorkflowStartResponse;

public record TaskStartResponse(
    String workflow,
    @JsonProperty("workflow_id")
    String workflowId,
    @JsonProperty("task_queue")
    String taskQueue,
    @JsonProperty("run_id")
    String runId
) {
    public static TaskStartResponse fromWorkflowStartResponse(final WorkflowStartResponse response) {
        return new TaskStartResponse(
            response.workflow(),
            response.workflowId(),
            response.taskQueue(),
            response.runId()
        );
    }
}
