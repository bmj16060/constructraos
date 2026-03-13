package net.mudpot.constructraos.commons.orchestration.system.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import net.mudpot.constructraos.commons.ai.model.LlmResponse;
import net.mudpot.constructraos.commons.ai.model.PromptBundle;
import net.mudpot.constructraos.commons.orchestration.system.model.HelloWorldResult;

@ActivityInterface
public interface HelloActivities {
    @ActivityMethod
    HelloWorldResult completeHello(String workflowId, String name, String useCase, PromptBundle prompt, LlmResponse llmResponse);
}
