package net.mudpot.constructraos.orchestration.system.codex.activities;

import jakarta.inject.Singleton;
import net.mudpot.constructraos.commons.orchestration.codex.execution.CodexExecutionAdapter;
import net.mudpot.constructraos.commons.orchestration.codex.activities.CodexExecutionActivities;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionActivityInput;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionResult;

@Singleton
public class CodexExecutionActivitiesImpl implements CodexExecutionActivities {
    private final CodexExecutionAdapter executionAdapter;

    public CodexExecutionActivitiesImpl(final CodexExecutionAdapter executionAdapter) {
        this.executionAdapter = executionAdapter;
    }

    @Override
    public CodexExecutionResult execute(final CodexExecutionActivityInput input) {
        return executionAdapter.execute(input);
    }
}
