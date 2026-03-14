package net.mudpot.constructraos.commons.orchestration.codex.execution;

import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionActivityInput;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionResult;

public interface CodexExecutionAdapter {
    CodexExecutionResult execute(CodexExecutionActivityInput input);
}
