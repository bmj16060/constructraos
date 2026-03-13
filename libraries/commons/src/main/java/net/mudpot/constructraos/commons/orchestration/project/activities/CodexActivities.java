package net.mudpot.constructraos.commons.orchestration.project.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchRequest;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchResult;

@ActivityInterface
public interface CodexActivities {
    @ActivityMethod
    CodexExecutionDispatchResult dispatchExecution(CodexExecutionDispatchRequest request);
}
