package net.mudpot.constructraos.orchestration.project.codex;

import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchRequest;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchResult;

public interface CodexDispatchClient {
    CodexExecutionDispatchResult dispatch(CodexExecutionDispatchRequest request);
}
