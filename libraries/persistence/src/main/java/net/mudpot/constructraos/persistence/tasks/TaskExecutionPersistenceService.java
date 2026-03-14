package net.mudpot.constructraos.persistence.tasks;

import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionActivityInput;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionOutcome;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionResult;

import java.nio.charset.StandardCharsets;
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
        final UUID projectId = stableUuid("project", rootPath);
        projectRepository.upsertByRootPath(projectId, derivedProjectName(rootPath), rootPath);

        final UUID taskId = stableUuid("task", input.workflowId());
        upsertTask(projectId, taskId, input, agentName);

        final UUID agentSessionId = stableUuid("agent-session", taskId.toString(), agentName);
        upsertAgentSession(taskId, agentSessionId, agentName);

        final UUID taskStepId = stableUuid("task-step", taskId.toString(), Integer.toString(FIRST_STEP_NUMBER));
        upsertTaskStep(taskId, taskStepId, agentSessionId, agentName);

        return new TaskExecutionContext(projectId, taskId, taskStepId, agentSessionId);
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
        taskStepRepository.update(taskStep);

        task.setStatus(result.status());
        task.setCompletedAt(completedAt);
        taskRepository.update(task);
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
        taskStepRepository.update(taskStep);

        task.setStatus(STATUS_FAILED);
        task.setCompletedAt(completedAt);
        taskRepository.update(task);
    }

    private void upsertTask(final UUID projectId, final UUID taskId, final CodexExecutionActivityInput input, final String agentName) {
        taskRepository.upsertByWorkflowId(
            taskId,
            projectId,
            input.workflowId(),
            sanitize(input.prompt()),
            STATUS_RUNNING,
            agentName,
            sanitize(input.actorKind()),
            sanitize(input.sessionId())
        );
    }

    private void upsertAgentSession(final UUID taskId, final UUID agentSessionId, final String agentName) {
        agentSessionRepository.upsertByTaskAndAgentName(agentSessionId, taskId, agentName, "");
    }

    private void upsertTaskStep(final UUID taskId, final UUID taskStepId, final UUID agentSessionId, final String agentName) {
        taskStepRepository.upsertByTaskAndStepNumber(
            taskStepId,
            taskId,
            FIRST_STEP_NUMBER,
            agentName,
            agentSessionId,
            STATUS_RUNNING
        );
    }

    private void updateAgentSession(final AgentSessionEntity entity, final String sessionId) {
        final String normalized = sanitize(sessionId);
        if (!normalized.isBlank()) {
            entity.setProviderSessionId(normalized);
            agentSessionRepository.update(entity);
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
        applyTranscriptValues(entity, task, taskStep, agentSession, sessionId, transcriptLines);
        if (entity.getId() == null) {
            transcriptRecordRepository.save(entity);
            return;
        }
        transcriptRecordRepository.update(entity);
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
        applyTaskStepResultValues(entity, task, taskStep, status, summary, recommendedNextAgent, resultPayload);
        if (entity.getId() == null) {
            taskStepResultRepository.save(entity);
            return;
        }
        taskStepResultRepository.update(entity);
    }

    private static void applyTranscriptValues(
        final TranscriptRecordEntity entity,
        final TaskEntity task,
        final TaskStepEntity taskStep,
        final AgentSessionEntity agentSession,
        final String sessionId,
        final List<String> transcriptLines
    ) {
        entity.setTaskId(task.getId());
        entity.setTaskStepId(taskStep.getId());
        entity.setAgentSessionId(agentSession.getId());
        entity.setTranscriptKind(TRANSCRIPT_KIND);
        entity.setProviderSessionId(sanitize(sessionId));
        entity.setTranscriptPayload(transcriptLines == null ? List.of() : List.copyOf(transcriptLines));
    }

    private static void applyTaskStepResultValues(
        final TaskStepResultEntity entity,
        final TaskEntity task,
        final TaskStepEntity taskStep,
        final String status,
        final String summary,
        final String recommendedNextAgent,
        final Map<String, Object> resultPayload
    ) {
        entity.setTaskId(task.getId());
        entity.setTaskStepId(taskStep.getId());
        entity.setStatus(status);
        entity.setSummary(summary);
        entity.setRecommendedNextAgent(recommendedNextAgent);
        entity.setResultPayload(resultPayload);
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

    private static UUID stableUuid(final String scope, final String... parts) {
        final String joined = scope + "::" + String.join("::", parts);
        return UUID.nameUUIDFromBytes(joined.getBytes(StandardCharsets.UTF_8));
    }

    private static String sanitize(final String value) {
        return value == null ? "" : value.trim();
    }
}
