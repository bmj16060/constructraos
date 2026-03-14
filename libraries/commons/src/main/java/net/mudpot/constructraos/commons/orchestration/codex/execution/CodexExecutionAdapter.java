package net.mudpot.constructraos.commons.orchestration.codex.execution;

import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionActivityInput;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionOutcome;

public interface CodexExecutionAdapter {
    CodexExecutionOutcome execute(CodexExecutionActivityInput input);
}
