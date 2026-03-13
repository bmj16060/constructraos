package net.mudpot.constructraos.orchestration.system.activities;

import jakarta.inject.Singleton;
import net.mudpot.constructraos.commons.ai.model.LlmResponse;
import net.mudpot.constructraos.commons.ai.model.PromptBundle;
import net.mudpot.constructraos.commons.orchestration.system.activities.HelloActivities;
import net.mudpot.constructraos.commons.orchestration.system.model.HelloWorldResult;
import net.mudpot.constructraos.persistence.history.PromptRunEntity;
import net.mudpot.constructraos.persistence.history.PromptRunRepository;

import java.time.Instant;

@Singleton
public class HelloActivitiesImpl implements HelloActivities {
    private final PromptRunRepository promptRunRepository;

    public HelloActivitiesImpl(final PromptRunRepository promptRunRepository) {
        this.promptRunRepository = promptRunRepository;
    }

    @Override
    public HelloWorldResult completeHello(
        final String workflowId,
        final String name,
        final String useCase,
        final PromptBundle prompt,
        final LlmResponse llmResponse
    ) {
        final String greeting = llmResponse.getText() == null ? "" : llmResponse.getText().trim();
        if (greeting.isBlank()) {
            throw new RuntimeException("LLM returned empty response");
        }

        final Instant createdAt = Instant.now();
        final PromptRunEntity entity = new PromptRunEntity();
        entity.setWorkflowId(workflowId);
        entity.setPromptTemplate(prompt.getTemplate());
        entity.setUserName(name == null || name.isBlank() ? "World" : name.trim());
        entity.setUseCase(useCase == null || useCase.isBlank() ? "Demonstrate the ConstructraOS platform baseline." : useCase.trim());
        entity.setPromptText(prompt.getUserPrompt());
        entity.setResponseText(greeting);
        entity.setProvider(llmResponse.getProvider());
        entity.setModel(llmResponse.getModel());
        entity.setCacheHit(Boolean.TRUE.equals(llmResponse.getCache().get("hit")));
        entity.setCreatedAt(createdAt);
        promptRunRepository.save(entity);

        return new HelloWorldResult(
            workflowId,
            prompt.getTemplate(),
            greeting,
            llmResponse.getProvider(),
            llmResponse.getModel(),
            llmResponse.getUsage(),
            llmResponse.getCache(),
            createdAt
        );
    }
}
