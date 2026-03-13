package net.mudpot.constructraos.orchestration.project.activities;

import jakarta.inject.Singleton;
import net.mudpot.constructraos.commons.orchestration.project.activities.CodexActivities;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchRequest;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchResult;

@Singleton
public class CodexActivitiesImpl implements CodexActivities {
    @Override
    public CodexExecutionDispatchResult dispatchExecution(final CodexExecutionDispatchRequest request) {
        return new CodexExecutionDispatchResult(
            request.executionRequestId(),
            "",
            "dispatched",
            "Codex execution request dispatched. Awaiting thread acceptance callback."
        );
    }
}
