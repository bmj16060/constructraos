package net.mudpot.constructraos.persistence.runtimecoordination;

import java.util.Optional;

public interface RuntimeCoordinationOperations {
    RuntimeExecutionContext beginExecution(RuntimeExecutionStartRequest request);

    void recordProgress(RuntimeExecutionContext context, RuntimeExecutionUpdate update);

    void recordTerminalState(RuntimeExecutionContext context, RuntimeExecutionTerminalState terminalState, RuntimeExecutionUpdate update, String failureReason);

    Optional<RuntimeExecutionView> findByWorkflowId(String workflowId);
}
