package net.mudpot.constructraos.commons.orchestration.ai.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import net.mudpot.constructraos.commons.ai.model.LlmResponse;

@ActivityInterface
public interface LlmActivities {
    @ActivityMethod
    LlmResponse callLlm(
        String userPrompt,
        String provider,
        String model,
        String systemPrompt,
        double temperature,
        int maxTokens,
        String cacheKey,
        int cacheTtlSeconds,
        boolean cacheByPromptHash
    );
}
