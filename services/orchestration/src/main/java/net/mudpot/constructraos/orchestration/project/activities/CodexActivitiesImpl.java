package net.mudpot.constructraos.orchestration.project.activities;

import jakarta.inject.Singleton;
import net.mudpot.constructraos.commons.orchestration.project.activities.CodexActivities;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchRequest;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchResult;
import net.mudpot.constructraos.orchestration.project.codex.CodexDispatchClient;

@Singleton
public class CodexActivitiesImpl implements CodexActivities {
    private final CodexDispatchClient codexDispatchClient;

    public CodexActivitiesImpl(final CodexDispatchClient codexDispatchClient) {
        this.codexDispatchClient = codexDispatchClient;
    }

    @Override
    public CodexExecutionDispatchResult dispatchExecution(final CodexExecutionDispatchRequest request) {
        return codexDispatchClient.dispatch(request);
    }
}
