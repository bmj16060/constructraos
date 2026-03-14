package net.mudpot.constructraos.persistence.runtimecoordination;

import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class RuntimeCoordinationService implements RuntimeCoordinationOperations {
    private static final String STATE_RUNNING = "running";

    private final RuntimeExecutionRepository runtimeExecutionRepository;
    private final RuntimeExecutionCheckpointRepository runtimeExecutionCheckpointRepository;

    public RuntimeCoordinationService(
        final RuntimeExecutionRepository runtimeExecutionRepository,
        final RuntimeExecutionCheckpointRepository runtimeExecutionCheckpointRepository
    ) {
        this.runtimeExecutionRepository = runtimeExecutionRepository;
        this.runtimeExecutionCheckpointRepository = runtimeExecutionCheckpointRepository;
    }

    @Override
    @Transactional
    public RuntimeExecutionContext beginExecution(final RuntimeExecutionStartRequest request) {
        final String workflowId = requiredValue(request.workflowId(), "workflowId");
        final UUID taskId = requiredUuid(request.taskId(), "taskId");
        final UUID taskStepId = requiredUuid(request.taskStepId(), "taskStepId");
        final String executionMode = normalizedExecutionMode(request.executionMode());
        final String checkpointKind = normalizedCheckpointKind(request.checkpointKind());
        final UUID runtimeExecutionId = stableUuid("runtime-execution", workflowId);

        runtimeExecutionRepository.upsertStarted(runtimeExecutionId, workflowId, taskId, taskStepId, executionMode, STATE_RUNNING);
        upsertCheckpoint(runtimeExecutionId, checkpointKind, Map.of(
            "task_id", taskId.toString(),
            "task_step_id", taskStepId.toString(),
            "execution_mode", executionMode
        ));
        return new RuntimeExecutionContext(runtimeExecutionId);
    }

    @Override
    @Transactional
    public void recordProgress(final RuntimeExecutionContext context, final RuntimeExecutionUpdate update) {
        final RuntimeExecutionEntity entity = requiredExecution(context);
        applyProgress(entity, update);
        runtimeExecutionRepository.update(entity);
        upsertCheckpoint(entity.getId(), normalizedCheckpointKind(update.checkpointKind()), update.checkpointPayload());
    }

    @Override
    @Transactional
    public void recordTerminalState(
        final RuntimeExecutionContext context,
        final RuntimeExecutionTerminalState terminalState,
        final RuntimeExecutionUpdate update,
        final String failureReason
    ) {
        final RuntimeExecutionEntity entity = requiredExecution(context);
        applyProgress(entity, update);
        entity.setState(terminalState.persistedValue());
        entity.setCompletedAt(Instant.now());
        entity.setFailureReason(sanitizeFailureReason(failureReason));
        runtimeExecutionRepository.update(entity);
        upsertCheckpoint(entity.getId(), normalizedCheckpointKind(update.checkpointKind()), update.checkpointPayload());
    }

    @Override
    @Transactional
    public Optional<RuntimeExecutionView> findByWorkflowId(final String workflowId) {
        return runtimeExecutionRepository.findByWorkflowId(sanitize(workflowId))
            .map(this::toView);
    }

    private RuntimeExecutionEntity requiredExecution(final RuntimeExecutionContext context) {
        final UUID runtimeExecutionId = requiredUuid(context.runtimeExecutionId(), "runtimeExecutionId");
        return runtimeExecutionRepository.findById(runtimeExecutionId)
            .orElseThrow(() -> new IllegalStateException("Missing runtime execution " + runtimeExecutionId));
    }

    private void applyProgress(final RuntimeExecutionEntity entity, final RuntimeExecutionUpdate update) {
        entity.setState(STATE_RUNNING);
        entity.setAwaitingApproval(false);
        entity.setProviderSessionId(sanitize(update.providerSessionId()));
        entity.setLastEventAt(Instant.now());
    }

    private void upsertCheckpoint(final UUID runtimeExecutionId, final String checkpointKind, final Map<String, Object> checkpointPayload) {
        final RuntimeExecutionCheckpointEntity entity = runtimeExecutionCheckpointRepository.findByRuntimeExecutionId(runtimeExecutionId)
            .orElseGet(RuntimeExecutionCheckpointEntity::new);
        entity.setRuntimeExecutionId(runtimeExecutionId);
        entity.setCheckpointKind(checkpointKind);
        entity.setCheckpointPayload(checkpointPayload == null ? Map.of() : Map.copyOf(checkpointPayload));
        entity.setCheckpointedAt(Instant.now());
        if (entity.getId() == null) {
            runtimeExecutionCheckpointRepository.save(entity);
            return;
        }
        runtimeExecutionCheckpointRepository.update(entity);
    }

    private RuntimeExecutionView toView(final RuntimeExecutionEntity entity) {
        final RuntimeExecutionCheckpointEntity checkpoint = runtimeExecutionCheckpointRepository.findByRuntimeExecutionId(entity.getId()).orElse(null);
        return new RuntimeExecutionView(
            entity.getId(),
            entity.getWorkflowId(),
            entity.getTaskId(),
            entity.getTaskStepId(),
            entity.getExecutionMode(),
            entity.getState(),
            entity.isAwaitingApproval(),
            sanitize(entity.getOwnerInstanceId()),
            sanitize(entity.getProviderSessionId()),
            sanitize(entity.getProviderThreadId()),
            sanitize(entity.getProviderTurnId()),
            sanitize(entity.getCurrentRequestId()),
            entity.getLastEventAt(),
            entity.getLastHeartbeatAt(),
            entity.getLeaseExpiresAt(),
            entity.getStartedAt(),
            entity.getCompletedAt(),
            sanitize(entity.getFailureReason()),
            checkpoint == null ? "" : sanitize(checkpoint.getCheckpointKind()),
            checkpoint == null || checkpoint.getCheckpointPayload() == null ? Map.of() : Map.copyOf(checkpoint.getCheckpointPayload()),
            checkpoint == null ? null : checkpoint.getCheckpointedAt()
        );
    }

    private static UUID stableUuid(final String scope, final String value) {
        return UUID.nameUUIDFromBytes((scope + "::" + value).getBytes(StandardCharsets.UTF_8));
    }

    private static UUID requiredUuid(final UUID value, final String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }

    private static String requiredValue(final String value, final String fieldName) {
        final String normalized = sanitize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return normalized;
    }

    private static String normalizedExecutionMode(final String value) {
        final String normalized = sanitize(value);
        return normalized.isBlank() ? "legacy-blocking" : normalized;
    }

    private static String normalizedCheckpointKind(final String value) {
        final String normalized = sanitize(value);
        return normalized.isBlank() ? "unspecified" : normalized;
    }

    private static String sanitizeFailureReason(final String value) {
        final String normalized = sanitize(value);
        return normalized.isBlank() ? null : normalized;
    }

    private static String sanitize(final String value) {
        return value == null ? "" : value.trim();
    }
}
