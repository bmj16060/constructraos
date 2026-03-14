package net.mudpot.constructraos.persistence.tasks;

import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionActivityInput;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionOutcome;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionResult;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Singleton
public class TaskExecutionPersistenceService implements TaskExecutionPersistenceOperations {
    private static final int FIRST_STEP_NUMBER = 1;
    private static final String STATUS_RUNNING = "running";
    private static final String STATUS_FAILED = "failed";
    private static final String TRANSCRIPT_KIND = "codex-cli-jsonl";

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TaskStepRepository taskStepRepository;
    private final AgentSessionRepository agentSessionRepository;
    private final TranscriptRecordRepository transcriptRecordRepository;
    private final TaskStepResultRepository taskStepResultRepository;

    public TaskExecutionPersistenceService(
        final ProjectRepository projectRepository,
        final TaskRepository taskRepository,
        final TaskStepRepository taskStepRepository,
        final AgentSessionRepository agentSessionRepository,
        final TranscriptRecordRepository transcriptRecordRepository,
        final TaskStepResultRepository taskStepResultRepository
    ) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.taskStepRepository = taskStepRepository;
        this.agentSessionRepository = agentSessionRepository;
        this.transcriptRecordRepository = transcriptRecordRepository;
        this.taskStepResultRepository = taskStepResultRepository;
    }

    @Override
    @Transactional
    public TaskExecutionContext beginExecution(final CodexExecutionActivityInput input) {
        final String rootPath = normalizedRootPath(input.workingDirectory());
        final String agentName = normalizedAgentName(input.agentName());
        final ProjectEntity project = projectRepository.findByRootPath(rootPath)
            .orElseGet(() -> projectRepository.save(newProject(rootPath)));
        final TaskEntity task = upsertTask(project, input, agentName);
        final AgentSessionEntity agentSession = upsertAgentSession(task.getId(), agentName);
        final TaskStepEntity taskStep = upsertTaskStep(task.getId(), agentSession.getId(), agentName);
        return new TaskExecutionContext(project.getId(), task.getId(), taskStep.getId(), agentSession.getId());
    }

    @Override
    @Transactional
    public void recordSuccess(final TaskExecutionContext context, final CodexExecutionOutcome outcome) {
        final Instant completedAt = Instant.now();
        final TaskEntity task = requiredTask(context.taskId());
        final TaskStepEntity taskStep = requiredTaskStep(context.taskStepId());
        final AgentSessionEntity agentSession = requiredAgentSession(context.agentSessionId());
        final CodexExecutionResult result = outcome.result();

        updateAgentSession(agentSession, outcome.sessionId());
        upsertTranscript(task, taskStep, agentSession, outcome.sessionId(), outcome.transcriptLines());
        upsertTaskStepResult(
            task,
            taskStep,
            result.status(),
            result.summary(),
            result.recommendedNextAgent(),
            Map.of(
                "status", result.status(),
                "summary", result.summary(),
                "recommended_next_agent", result.recommendedNextAgent()
            )
        );

        taskStep.setStatus(result.status());
        taskStep.setCompletedAt(completedAt);
        taskStepRepository.save(taskStep);

        task.setStatus(result.status());
        task.setCompletedAt(completedAt);
        taskRepository.save(task);
    }

    @Override
    @Transactional
    public void recordFailure(
        final TaskExecutionContext context,
        final String errorMessage,
        final String sessionId,
        final List<String> transcriptLines
    ) {
        final Instant completedAt = Instant.now();
        final TaskEntity task = requiredTask(context.taskId());
        final TaskStepEntity taskStep = requiredTaskStep(context.taskStepId());
        final AgentSessionEntity agentSession = requiredAgentSession(context.agentSessionId());
        final String summary = normalizedFailureMessage(errorMessage);

        updateAgentSession(agentSession, sessionId);
        upsertTranscript(task, taskStep, agentSession, sessionId, transcriptLines);
        upsertTaskStepResult(
            task,
            taskStep,
            STATUS_FAILED,
            summary,
            "none",
            Map.of(
                "status", STATUS_FAILED,
                "summary", summary,
                "recommended_next_agent", "none",
                "error", summary
            )
        );

        taskStep.setStatus(STATUS_FAILED);
        taskStep.setCompletedAt(completedAt);
        taskStepRepository.save(taskStep);

        task.setStatus(STATUS_FAILED);
        task.setCompletedAt(completedAt);
        taskRepository.save(task);
    }

    private ProjectEntity newProject(final String rootPath) {
        final ProjectEntity entity = new ProjectEntity();
        entity.setName(derivedProjectName(rootPath));
        entity.setRootPath(rootPath);
        return entity;
    }

    private TaskEntity upsertTask(final ProjectEntity project, final CodexExecutionActivityInput input, final String agentName) {
        final TaskEntity entity = taskRepository.findByWorkflowId(input.workflowId()).orElseGet(TaskEntity::new);
        entity.setProjectId(project.getId());
        entity.setWorkflowId(input.workflowId());
        entity.setGoal(sanitize(input.prompt()));
        entity.setStatus(STATUS_RUNNING);
        entity.setRequestedAgentName(agentName);
        entity.setRequestedByKind(sanitize(input.actorKind()));
        entity.setRequestedBySessionId(sanitize(input.sessionId()));
        entity.setCompletedAt(null);
        return taskRepository.save(entity);
    }

    private AgentSessionEntity upsertAgentSession(final UUID taskId, final String agentName) {
        final AgentSessionEntity entity = agentSessionRepository.findByTaskIdAndAgentName(taskId, agentName).orElseGet(AgentSessionEntity::new);
        entity.setTaskId(taskId);
        entity.setAgentName(agentName);
        return agentSessionRepository.save(entity);
    }

    private TaskStepEntity upsertTaskStep(final UUID taskId, final UUID agentSessionId, final String agentName) {
        final TaskStepEntity entity = taskStepRepository.findByTaskIdAndStepNumber(taskId, FIRST_STEP_NUMBER).orElseGet(TaskStepEntity::new);
        entity.setTaskId(taskId);
        entity.setStepNumber(FIRST_STEP_NUMBER);
        entity.setAgentName(agentName);
        entity.setAgentSessionId(agentSessionId);
        entity.setStatus(STATUS_RUNNING);
        entity.setCompletedAt(null);
        return taskStepRepository.save(entity);
    }

    private void updateAgentSession(final AgentSessionEntity entity, final String sessionId) {
        final String normalized = sanitize(sessionId);
        if (!normalized.isBlank()) {
            entity.setProviderSessionId(normalized);
            agentSessionRepository.save(entity);
        }
    }

    private void upsertTranscript(
        final TaskEntity task,
        final TaskStepEntity taskStep,
        final AgentSessionEntity agentSession,
        final String sessionId,
        final List<String> transcriptLines
    ) {
        final TranscriptRecordEntity entity = transcriptRecordRepository.findByTaskStepId(taskStep.getId()).orElseGet(TranscriptRecordEntity::new);
        entity.setTaskId(task.getId());
        entity.setTaskStepId(taskStep.getId());
        entity.setAgentSessionId(agentSession.getId());
        entity.setTranscriptKind(TRANSCRIPT_KIND);
        entity.setProviderSessionId(sanitize(sessionId));
        entity.setTranscriptPayload(transcriptLines == null ? List.of() : List.copyOf(transcriptLines));
        transcriptRecordRepository.save(entity);
    }

    private void upsertTaskStepResult(
        final TaskEntity task,
        final TaskStepEntity taskStep,
        final String status,
        final String summary,
        final String recommendedNextAgent,
        final Map<String, Object> resultPayload
    ) {
        final TaskStepResultEntity entity = taskStepResultRepository.findByTaskStepId(taskStep.getId()).orElseGet(TaskStepResultEntity::new);
        entity.setTaskId(task.getId());
        entity.setTaskStepId(taskStep.getId());
        entity.setStatus(status);
        entity.setSummary(summary);
        entity.setRecommendedNextAgent(recommendedNextAgent);
        entity.setResultPayload(resultPayload);
        taskStepResultRepository.save(entity);
    }

    private TaskEntity requiredTask(final UUID taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> new IllegalStateException("Missing task " + taskId));
    }

    private TaskStepEntity requiredTaskStep(final UUID taskStepId) {
        return taskStepRepository.findById(taskStepId).orElseThrow(() -> new IllegalStateException("Missing task step " + taskStepId));
    }

    private AgentSessionEntity requiredAgentSession(final UUID agentSessionId) {
        return agentSessionRepository.findById(agentSessionId).orElseThrow(() -> new IllegalStateException("Missing agent session " + agentSessionId));
    }

    private static String normalizedRootPath(final String workingDirectory) {
        final String normalized = sanitize(workingDirectory);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Working directory is required.");
        }
        return Path.of(normalized).toAbsolutePath().normalize().toString();
    }

    private static String normalizedAgentName(final String agentName) {
        final String normalized = sanitize(agentName);
        return normalized.isBlank() ? "planner" : normalized;
    }

    private static String normalizedFailureMessage(final String value) {
        final String normalized = sanitize(value);
        return normalized.isBlank() ? "Codex execution failed." : normalized;
    }

    private static String derivedProjectName(final String rootPath) {
        final Path fileName = Path.of(rootPath).getFileName();
        return fileName == null ? rootPath : fileName.toString();
    }

    private static String sanitize(final String value) {
        return value == null ? "" : value.trim();
    }
}
