package net.mudpot.constructraos.commons.orchestration.system.workflows;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import net.mudpot.constructraos.commons.orchestration.system.model.HelloWorldResult;
import net.mudpot.constructraos.commons.orchestration.system.model.HelloWorldWorkflowInput;

@WorkflowInterface
public interface HelloWorldWorkflow {
    @WorkflowMethod(name = "HelloWorldWorkflow")
    HelloWorldResult run(HelloWorldWorkflowInput input);
}
