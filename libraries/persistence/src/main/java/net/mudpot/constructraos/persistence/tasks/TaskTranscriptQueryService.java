package net.mudpot.constructraos.persistence.tasks;

import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;

@Singleton
public class TaskTranscriptQueryService {
    private final TaskRepository taskRepository;
    private final TaskStepRepository taskStepRepository;
    private final TranscriptRecordRepository transcriptRecordRepository;

    public TaskTranscriptQueryService(
        final TaskRepository taskRepository,
        final TaskStepRepository taskStepRepository,
        final TranscriptRecordRepository transcriptRecordRepository
    ) {
        this.taskRepository = taskRepository;
        this.taskStepRepository = taskStepRepository;
        this.transcriptRecordRepository = transcriptRecordRepository;
    }

    public Optional<TaskTranscriptView> findLatestByWorkflowId(final String workflowId) {
        return taskRepository.findByWorkflowId(sanitize(workflowId))
            .flatMap(task -> taskStepRepository.findLatestByTaskId(task.getId())
                .flatMap(step -> transcriptRecordRepository.findByTaskStepId(step.getId())
                    .map(transcript -> new TaskTranscriptView(
                        task.getId(),
                        task.getWorkflowId(),
                        transcript.getId(),
                        step.getStepNumber(),
                        step.getStatus(),
                        step.getAgentName(),
                        transcript.getTranscriptKind(),
                        transcript.getProviderSessionId(),
                        transcript.getTranscriptPayload() == null ? List.of() : List.copyOf(transcript.getTranscriptPayload()),
                        transcript.getCreatedAt(),
                        transcript.getUpdatedAt()
                    ))));
    }

    private static String sanitize(final String value) {
        return value == null ? "" : value.trim();
    }
}
