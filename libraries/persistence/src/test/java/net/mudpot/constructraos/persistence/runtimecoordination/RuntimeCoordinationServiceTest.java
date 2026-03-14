package net.mudpot.constructraos.persistence.runtimecoordination;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeCoordinationServiceTest {
    @Test
    void beginExecutionCreatesRunningExecutionAndCheckpoint() {
        final AtomicReference<RuntimeExecutionEntity> executionStore = new AtomicReference<>();
        final AtomicReference<RuntimeExecutionCheckpointEntity> checkpointStore = new AtomicReference<>();

        final RuntimeCoordinationService service = new RuntimeCoordinationService(
            runtimeExecutionRepository(executionStore),
            runtimeExecutionCheckpointRepository(checkpointStore)
        );

        final RuntimeExecutionContext context = service.beginExecution(new RuntimeExecutionStartRequest(
            "wf-123",
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            "legacy-blocking",
            "execution-started"
        ));

        final Optional<RuntimeExecutionView> view = service.findByWorkflowId("wf-123");

        assertNotNull(context.runtimeExecutionId());
        assertTrue(view.isPresent());
        assertEquals("running", view.get().state());
        assertEquals("legacy-blocking", view.get().executionMode());
        assertEquals("execution-started", view.get().checkpointKind());
        assertEquals("11111111-1111-1111-1111-111111111111", view.get().checkpointPayload().get("task_id"));
    }

    @Test
    void recordTerminalStateUpdatesFailureAndCheckpoint() {
        final AtomicReference<RuntimeExecutionEntity> executionStore = new AtomicReference<>();
        final AtomicReference<RuntimeExecutionCheckpointEntity> checkpointStore = new AtomicReference<>();
        final UUID runtimeExecutionId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        final RuntimeExecutionEntity execution = new RuntimeExecutionEntity();
        execution.setId(runtimeExecutionId);
        execution.setWorkflowId("wf-456");
        execution.setTaskId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        execution.setTaskStepId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        execution.setExecutionMode("legacy-blocking");
        execution.setState("running");
        execution.setStartedAt(Instant.parse("2026-03-14T10:00:00Z"));
        executionStore.set(execution);

        final RuntimeCoordinationService service = new RuntimeCoordinationService(
            runtimeExecutionRepository(executionStore),
            runtimeExecutionCheckpointRepository(checkpointStore)
        );

        service.recordTerminalState(
            new RuntimeExecutionContext(runtimeExecutionId),
            RuntimeExecutionTerminalState.FAILED,
            new RuntimeExecutionUpdate(
                "thread-123",
                "execution-failed",
                Map.of("error", "Unauthorized")
            ),
            "Unauthorized"
        );

        final Optional<RuntimeExecutionView> view = service.findByWorkflowId("wf-456");

        assertTrue(view.isPresent());
        assertEquals("failed", view.get().state());
        assertEquals("thread-123", view.get().providerSessionId());
        assertEquals("Unauthorized", view.get().failureReason());
        assertEquals("execution-failed", view.get().checkpointKind());
        assertEquals("Unauthorized", view.get().checkpointPayload().get("error"));
        assertNotNull(view.get().completedAt());
    }

    @SuppressWarnings("unchecked")
    private static RuntimeExecutionRepository runtimeExecutionRepository(final AtomicReference<RuntimeExecutionEntity> executionStore) {
        return (RuntimeExecutionRepository) Proxy.newProxyInstance(
            RuntimeExecutionRepository.class.getClassLoader(),
            new Class<?>[] {RuntimeExecutionRepository.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "findById" -> Optional.ofNullable(executionStore.get());
                case "findByWorkflowId" -> {
                    final RuntimeExecutionEntity entity = executionStore.get();
                    final String workflowId = (String) args[0];
                    yield Optional.ofNullable(entity != null && workflowId.equals(entity.getWorkflowId()) ? entity : null);
                }
                case "upsertStarted" -> {
                    final RuntimeExecutionEntity entity = new RuntimeExecutionEntity();
                    entity.setId((UUID) args[0]);
                    entity.setWorkflowId((String) args[1]);
                    entity.setTaskId((UUID) args[2]);
                    entity.setTaskStepId((UUID) args[3]);
                    entity.setExecutionMode((String) args[4]);
                    entity.setState((String) args[5]);
                    entity.setStartedAt(Instant.now());
                    entity.setLastEventAt(Instant.now());
                    executionStore.set(entity);
                    yield null;
                }
                case "update", "save" -> {
                    executionStore.set((RuntimeExecutionEntity) args[0]);
                    yield args[0];
                }
                case "toString" -> "RuntimeExecutionRepositoryProxy";
                default -> throw new UnsupportedOperationException(method.getName());
            }
        );
    }

    @SuppressWarnings("unchecked")
    private static RuntimeExecutionCheckpointRepository runtimeExecutionCheckpointRepository(
        final AtomicReference<RuntimeExecutionCheckpointEntity> checkpointStore
    ) {
        return (RuntimeExecutionCheckpointRepository) Proxy.newProxyInstance(
            RuntimeExecutionCheckpointRepository.class.getClassLoader(),
            new Class<?>[] {RuntimeExecutionCheckpointRepository.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "findByRuntimeExecutionId" -> {
                    final RuntimeExecutionCheckpointEntity entity = checkpointStore.get();
                    final UUID runtimeExecutionId = (UUID) args[0];
                    yield Optional.ofNullable(entity != null && runtimeExecutionId.equals(entity.getRuntimeExecutionId()) ? entity : null);
                }
                case "save", "update" -> {
                    final RuntimeExecutionCheckpointEntity entity = (RuntimeExecutionCheckpointEntity) args[0];
                    if (entity.getId() == null) {
                        entity.setId(UUID.randomUUID());
                    }
                    checkpointStore.set(entity);
                    yield entity;
                }
                case "toString" -> "RuntimeExecutionCheckpointRepositoryProxy";
                default -> throw new UnsupportedOperationException(method.getName());
            }
        );
    }
}
