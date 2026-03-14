package net.mudpot.constructraos.persistence.tasks;

import jakarta.inject.Singleton;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class TaskStatusQueryService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TaskStepRepository taskStepRepository;
    private final TaskStepResultRepository taskStepResultRepository;
    private final AgentSessionRepository agentSessionRepository;
    private final TranscriptRecordRepository transcriptRecordRepository;

    public TaskStatusQueryService(
        final TaskRepository taskRepository,
        final ProjectRepository projectRepository,
        final TaskStepRepository taskStepRepository,
        final TaskStepResultRepository taskStepResultRepository,
        final AgentSessionRepository agentSessionRepository,
        final TranscriptRecordRepository transcriptRecordRepository
    ) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.taskStepRepository = taskStepRepository;
        this.taskStepResultRepository = taskStepResultRepository;
        this.agentSessionRepository = agentSessionRepository;
        this.transcriptRecordRepository = transcriptRecordRepository;
    }

    public Optional<TaskStatusView> findByWorkflowId(final String workflowId) {
        return taskRepository.findByWorkflowId(workflowId).map(task -> {
            final ProjectEntity project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> new IllegalStateException("Missing project " + task.getProjectId()));
            final Optional<TaskStepEntity> latestStep = taskStepRepository.findLatestByTaskId(task.getId());
            final TaskStepEntity step = latestStep.orElse(null);
            final TaskStepResultEntity result = step == null ? null : taskStepResultRepository.findByTaskStepId(step.getId()).orElse(null);
            final AgentSessionEntity agentSession = step == null ? null : agentSessionRepository.findById(step.getAgentSessionId()).orElse(null);
            final TranscriptRecordEntity transcript = step == null ? null : transcriptRecordRepository.findByTaskStepId(step.getId()).orElse(null);
            return new TaskStatusView(
                task.getId(),
                task.getWorkflowId(),
                project.getName(),
                project.getRootPath(),
                task.getStatus(),
                task.getGoal(),
                task.getRequestedAgentName(),
                task.getRequestedByKind(),
                task.getRequestedBySessionId(),
                step == null ? null : step.getStepNumber(),
                step == null ? null : step.getStatus(),
                step == null ? null : step.getAgentName(),
                result == null ? null : result.getSummary(),
                result == null ? null : result.getRecommendedNextAgent(),
                agentSession == null ? "" : agentSession.getProviderSessionId(),
                transcript == null ? null : transcript.getId(),
                result == null ? Map.of() : result.getResultPayload(),
                task.getCreatedAt(),
                task.getCompletedAt()
            );
        });
    }

    public Optional<TaskStatusView> findByTaskId(final UUID taskId) {
        return taskRepository.findById(taskId).flatMap(task -> findByWorkflowId(task.getWorkflowId()));
    }
}
