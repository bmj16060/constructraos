package net.mudpot.constructraos.orchestration.system.codex.activities;

import jakarta.inject.Singleton;
import net.mudpot.constructraos.commons.orchestration.codex.execution.CodexExecutionAdapter;
import net.mudpot.constructraos.commons.orchestration.codex.execution.CodexExecutionException;
import net.mudpot.constructraos.commons.orchestration.codex.activities.CodexExecutionActivities;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionActivityInput;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionOutcome;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionResult;
import net.mudpot.constructraos.persistence.tasks.TaskExecutionContext;
import net.mudpot.constructraos.persistence.tasks.TaskExecutionPersistenceOperations;

import java.util.List;

@Singleton
public class CodexExecutionActivitiesImpl implements CodexExecutionActivities {
    private final CodexExecutionAdapter executionAdapter;
    private final TaskExecutionPersistenceOperations persistenceOperations;

    public CodexExecutionActivitiesImpl(
        final CodexExecutionAdapter executionAdapter,
        final TaskExecutionPersistenceOperations persistenceOperations
    ) {
        this.executionAdapter = executionAdapter;
        this.persistenceOperations = persistenceOperations;
    }

    @Override
    public CodexExecutionResult execute(final CodexExecutionActivityInput input) {
        final TaskExecutionContext context = persistenceOperations.beginExecution(input);
        try {
            final CodexExecutionOutcome outcome = executionAdapter.execute(input);
            persistenceOperations.recordSuccess(context, outcome);
            return outcome.result();
        } catch (final CodexExecutionException exception) {
            persistenceOperations.recordFailure(context, exception.getMessage(), exception.sessionId(), exception.transcriptLines());
            throw exception;
        } catch (final RuntimeException exception) {
            persistenceOperations.recordFailure(context, exception.getMessage(), "", List.of());
            throw exception;
        }
    }
}
