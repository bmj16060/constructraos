package net.mudpot.constructraos.persistence.tasks;

import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionActivityInput;
import net.mudpot.constructraos.persistence.runtimecoordination.RuntimeCoordinationOperations;
import net.mudpot.constructraos.persistence.runtimecoordination.RuntimeExecutionContext;
import net.mudpot.constructraos.persistence.runtimecoordination.RuntimeExecutionStartRequest;
import net.mudpot.constructraos.persistence.runtimecoordination.RuntimeExecutionTerminalState;
import net.mudpot.constructraos.persistence.runtimecoordination.RuntimeExecutionUpdate;
import net.mudpot.constructraos.persistence.runtimecoordination.RuntimeExecutionView;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TaskExecutionPersistenceServiceTest {
    @Test
    void beginExecutionAllocatesRuntimeExecutionContext() {
        final CapturingRuntimeCoordinationOperations runtimeCoordinationOperations = new CapturingRuntimeCoordinationOperations();
        final TaskExecutionPersistenceService service = new TaskExecutionPersistenceService(
            repositoryProxy(ProjectRepository.class),
            repositoryProxy(TaskRepository.class),
            repositoryProxy(TaskStepRepository.class),
            repositoryProxy(AgentSessionRepository.class),
            repositoryProxy(TranscriptRecordRepository.class),
            repositoryProxy(TaskStepResultRepository.class),
            runtimeCoordinationOperations
        );

        final TaskExecutionContext context = service.beginExecution(new CodexExecutionActivityInput(
            "wf-123",
            "Plan the next slice.",
            "/workspace/ConstructraOS",
            "planner",
            "anonymous",
            "anon-session-1"
        ));

        assertNotNull(context.projectId());
        assertNotNull(context.taskId());
        assertNotNull(context.taskStepId());
        assertEquals(runtimeCoordinationOperations.runtimeExecutionId, context.runtimeExecutionId());
        assertEquals("wf-123", runtimeCoordinationOperations.lastStartRequest.workflowId());
        assertEquals("legacy-blocking", runtimeCoordinationOperations.lastStartRequest.executionMode());
        assertEquals("execution-started", runtimeCoordinationOperations.lastStartRequest.checkpointKind());
    }

    @SuppressWarnings("unchecked")
    private static <T> T repositoryProxy(final Class<T> repositoryType) {
        return (T) Proxy.newProxyInstance(
            repositoryType.getClassLoader(),
            new Class<?>[] {repositoryType},
            (proxy, method, args) -> switch (method.getName()) {
                case "upsertByRootPath", "upsertByWorkflowId", "upsertByTaskAndAgentName", "upsertByTaskAndStepNumber" -> null;
                case "toString" -> repositoryType.getSimpleName() + "Proxy";
                default -> throw new UnsupportedOperationException(method.getName());
            }
        );
    }

    private static final class CapturingRuntimeCoordinationOperations implements RuntimeCoordinationOperations {
        private final UUID runtimeExecutionId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        private RuntimeExecutionStartRequest lastStartRequest;

        @Override
        public RuntimeExecutionContext beginExecution(final RuntimeExecutionStartRequest request) {
            this.lastStartRequest = request;
            return new RuntimeExecutionContext(runtimeExecutionId);
        }

        @Override
        public void recordProgress(final RuntimeExecutionContext context, final RuntimeExecutionUpdate update) {
            throw new UnsupportedOperationException("recordProgress");
        }

        @Override
        public void recordTerminalState(
            final RuntimeExecutionContext context,
            final RuntimeExecutionTerminalState terminalState,
            final RuntimeExecutionUpdate update,
            final String failureReason
        ) {
            throw new UnsupportedOperationException("recordTerminalState");
        }

        @Override
        public Optional<RuntimeExecutionView> findByWorkflowId(final String workflowId) {
            return Optional.empty();
        }
    }
}
