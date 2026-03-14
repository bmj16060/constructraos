package net.mudpot.constructraos.persistence.tasks;

import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionActivityInput;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionOutcome;

import java.util.List;

public interface TaskExecutionPersistenceOperations {
    TaskExecutionContext beginExecution(CodexExecutionActivityInput input);

    void recordSuccess(TaskExecutionContext context, CodexExecutionOutcome outcome);

    void recordFailure(TaskExecutionContext context, String errorMessage, String sessionId, List<String> transcriptLines);
}
