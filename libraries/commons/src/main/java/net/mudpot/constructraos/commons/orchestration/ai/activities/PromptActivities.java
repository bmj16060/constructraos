package net.mudpot.constructraos.commons.orchestration.ai.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import net.mudpot.constructraos.commons.ai.model.PromptBundle;

import java.util.Map;

@ActivityInterface
public interface PromptActivities {
    @ActivityMethod
    PromptBundle renderPrompt(String templateName, Map<String, Object> variables);
}
