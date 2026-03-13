package net.mudpot.constructraos.orchestration.project.activities;

import jakarta.inject.Singleton;
import net.mudpot.constructraos.commons.orchestration.project.activities.CodexActivities;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchRequest;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchResult;
import net.mudpot.constructraos.orchestration.project.codex.CodexBridgeClient;

@Singleton
public class CodexActivitiesImpl implements CodexActivities {
    private final CodexBridgeClient codexBridgeClient;

    public CodexActivitiesImpl(final CodexBridgeClient codexBridgeClient) {
        this.codexBridgeClient = codexBridgeClient;
    }

    @Override
    public CodexExecutionDispatchResult dispatchExecution(final CodexExecutionDispatchRequest request) {
        return codexBridgeClient.dispatch(request);
    }
}
