package net.mudpot.constructraos.commons.orchestration.project.workflows;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionAcceptedSignal;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskQaRequestSignal;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskSreEnvironmentOutcomeSignal;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowInput;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowState;

@WorkflowInterface
public interface TaskCoordinationWorkflow {
    @WorkflowMethod
    void run(TaskWorkflowInput input);

    @SignalMethod
    void requestQa(TaskQaRequestSignal request);

    @SignalMethod
    void reportCodexExecutionAccepted(CodexExecutionAcceptedSignal accepted);

    @SignalMethod
    void reportSreEnvironmentOutcome(TaskSreEnvironmentOutcomeSignal outcome);

    @SignalMethod
    void close(String reason);

    @QueryMethod
    TaskWorkflowState currentState();
}
