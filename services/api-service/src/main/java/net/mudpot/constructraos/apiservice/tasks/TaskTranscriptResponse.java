package net.mudpot.constructraos.apiservice.tasks;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.mudpot.constructraos.persistence.tasks.TaskTranscriptView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TaskTranscriptResponse(
    @JsonProperty("task_id")
    UUID taskId,
    @JsonProperty("workflow_id")
    String workflowId,
    @JsonProperty("transcript_record_id")
    UUID transcriptRecordId,
    @JsonProperty("latest_step_number")
    Integer latestStepNumber,
    @JsonProperty("latest_step_status")
    String latestStepStatus,
    @JsonProperty("latest_step_agent_name")
    String latestStepAgentName,
    @JsonProperty("transcript_kind")
    String transcriptKind,
    @JsonProperty("provider_session_id")
    String providerSessionId,
    @JsonProperty("transcript_payload")
    List<String> transcriptPayload,
    @JsonProperty("created_at")
    Instant createdAt,
    @JsonProperty("updated_at")
    Instant updatedAt
) {
    public static TaskTranscriptResponse fromView(final TaskTranscriptView view) {
        return new TaskTranscriptResponse(
            view.taskId(),
            view.workflowId(),
            view.transcriptRecordId(),
            view.latestStepNumber(),
            view.latestStepStatus(),
            view.latestStepAgentName(),
            view.transcriptKind(),
            view.providerSessionId(),
            view.transcriptPayload(),
            view.createdAt(),
            view.updatedAt()
        );
    }
}
