package net.mudpot.constructraos.orchestration.system.codex.activities;

import net.mudpot.constructraos.commons.orchestration.codex.execution.CodexExecutionAdapter;
import net.mudpot.constructraos.commons.orchestration.codex.execution.CodexExecutionException;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionActivityInput;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionOutcome;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionResult;
import net.mudpot.constructraos.persistence.tasks.TaskExecutionContext;
import net.mudpot.constructraos.persistence.tasks.TaskExecutionPersistenceOperations;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CodexExecutionActivitiesImplTest {
    @Test
    void executePersistsSuccessfulOutcome() {
        final StubPersistenceOperations persistenceOperations = new StubPersistenceOperations();
        final CodexExecutionActivitiesImpl activities = new CodexExecutionActivitiesImpl(
            input -> new CodexExecutionOutcome(
                new CodexExecutionResult("completed", "Execution persisted.", "reviewer"),
                "thread-123",
                List.of("{\"type\":\"thread.started\",\"thread_id\":\"thread-123\"}")
            ),
            persistenceOperations
        );

        final CodexExecutionResult result = activities.execute(new CodexExecutionActivityInput(
            "wf-1",
            "Summarize the next step.",
            "/workspace",
            "planner",
            "anonymous",
            "anon-session-1"
        ));

        assertEquals("completed", result.status());
        assertEquals("thread-123", persistenceOperations.lastSuccessOutcome.sessionId());
        assertEquals("wf-1", persistenceOperations.lastInput.workflowId());
    }

    @Test
    void executePersistsFailedOutcomeBeforeRethrow() {
        final StubPersistenceOperations persistenceOperations = new StubPersistenceOperations();
        final CodexExecutionActivitiesImpl activities = new CodexExecutionActivitiesImpl(
            input -> {
                throw new CodexExecutionException(
                    "Unauthorized",
                    "thread-123",
                    List.of("{\"type\":\"turn.failed\",\"error\":{\"message\":\"Unauthorized\"}}")
                );
            },
            persistenceOperations
        );

        final CodexExecutionException exception = assertThrows(
            CodexExecutionException.class,
            () -> activities.execute(new CodexExecutionActivityInput(
                "wf-1",
                "Summarize the next step.",
                "/workspace",
                "planner",
                "anonymous",
                "anon-session-1"
            ))
        );

        assertEquals("Unauthorized", exception.getMessage());
        assertEquals("Unauthorized", persistenceOperations.lastFailureMessage);
        assertEquals("thread-123", persistenceOperations.lastFailureSessionId);
        assertEquals(1, persistenceOperations.lastFailureTranscriptLines.size());
    }

    private static final class StubPersistenceOperations implements TaskExecutionPersistenceOperations {
        private final TaskExecutionContext context = new TaskExecutionContext(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            UUID.fromString("33333333-3333-3333-3333-333333333333"),
            UUID.fromString("44444444-4444-4444-4444-444444444444"),
            UUID.fromString("55555555-5555-5555-5555-555555555555")
        );

        private CodexExecutionActivityInput lastInput;
        private CodexExecutionOutcome lastSuccessOutcome;
        private String lastFailureMessage;
        private String lastFailureSessionId;
        private List<String> lastFailureTranscriptLines;

        @Override
        public TaskExecutionContext beginExecution(final CodexExecutionActivityInput input) {
            this.lastInput = input;
            return context;
        }

        @Override
        public void recordSuccess(final TaskExecutionContext context, final CodexExecutionOutcome outcome) {
            this.lastSuccessOutcome = outcome;
        }

        @Override
        public void recordFailure(
            final TaskExecutionContext context,
            final String errorMessage,
            final String sessionId,
            final List<String> transcriptLines
        ) {
            this.lastFailureMessage = errorMessage;
            this.lastFailureSessionId = sessionId;
            this.lastFailureTranscriptLines = transcriptLines;
        }
    }
}
