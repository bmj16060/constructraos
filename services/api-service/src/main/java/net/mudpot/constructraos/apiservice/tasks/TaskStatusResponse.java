package net.mudpot.constructraos.apiservice.tasks;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.mudpot.constructraos.persistence.tasks.TaskStatusView;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TaskStatusResponse(
    @JsonProperty("task_id")
    UUID taskId,
    @JsonProperty("workflow_id")
    String workflowId,
    @JsonProperty("project_name")
    String projectName,
    @JsonProperty("project_root_path")
    String projectRootPath,
    String status,
    String goal,
    @JsonProperty("requested_agent_name")
    String requestedAgentName,
    @JsonProperty("requested_by_kind")
    String requestedByKind,
    @JsonProperty("requested_by_session_id")
    String requestedBySessionId,
    @JsonProperty("latest_step_number")
    Integer latestStepNumber,
    @JsonProperty("latest_step_status")
    String latestStepStatus,
    @JsonProperty("latest_step_agent_name")
    String latestStepAgentName,
    @JsonProperty("latest_step_summary")
    String latestStepSummary,
    @JsonProperty("recommended_next_agent")
    String recommendedNextAgent,
    @JsonProperty("provider_session_id")
    String providerSessionId,
    @JsonProperty("transcript_record_id")
    UUID transcriptRecordId,
    @JsonProperty("result_payload")
    Map<String, Object> resultPayload,
    @JsonProperty("created_at")
    Instant createdAt,
    @JsonProperty("completed_at")
    Instant completedAt
) {
    public static TaskStatusResponse fromView(final TaskStatusView view) {
        return new TaskStatusResponse(
            view.taskId(),
            view.workflowId(),
            view.projectName(),
            view.projectRootPath(),
            view.status(),
            view.goal(),
            view.requestedAgentName(),
            view.requestedByKind(),
            view.requestedBySessionId(),
            view.latestStepNumber(),
            view.latestStepStatus(),
            view.latestStepAgentName(),
            view.latestStepSummary(),
            view.recommendedNextAgent(),
            view.providerSessionId(),
            view.transcriptRecordId(),
            view.resultPayload(),
            view.createdAt(),
            view.completedAt()
        );
    }
}
