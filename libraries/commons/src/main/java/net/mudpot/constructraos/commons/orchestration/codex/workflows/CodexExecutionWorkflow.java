package net.mudpot.constructraos.commons.orchestration.codex.workflows;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionResult;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionWorkflowInput;

@WorkflowInterface
public interface CodexExecutionWorkflow {
    @WorkflowMethod(name = "CodexExecutionWorkflow")
    CodexExecutionResult run(CodexExecutionWorkflowInput input);
}
