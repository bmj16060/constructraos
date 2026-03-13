package net.mudpot.constructraos.commons.orchestration.project.activities;

import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchRequest;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchResult;

public interface CodexActivities {
    CodexExecutionDispatchResult dispatchExecution(CodexExecutionDispatchRequest request);
}
