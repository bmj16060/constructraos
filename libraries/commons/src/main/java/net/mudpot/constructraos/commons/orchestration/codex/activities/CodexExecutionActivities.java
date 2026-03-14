package net.mudpot.constructraos.commons.orchestration.codex.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionActivityInput;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionResult;

@ActivityInterface
public interface CodexExecutionActivities {
    @ActivityMethod
    CodexExecutionResult execute(CodexExecutionActivityInput input);
}
