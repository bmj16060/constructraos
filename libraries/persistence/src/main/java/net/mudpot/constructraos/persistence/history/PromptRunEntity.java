package net.mudpot.constructraos.persistence.history;

import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.Id;

import java.time.Instant;
import java.util.UUID;

@MappedEntity("prompt_runs")
public class PromptRunEntity {
    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("workflow_id")
    private String workflowId;

    @MappedProperty("prompt_template")
    private String promptTemplate;

    @MappedProperty("user_name")
    private String userName;

    @MappedProperty("use_case")
    private String useCase;

    @MappedProperty("prompt_text")
    private String promptText;

    @MappedProperty("response_text")
    private String responseText;

    private String provider;

    private String model;

    @MappedProperty("cache_hit")
    private boolean cacheHit;

    @DateCreated
    @MappedProperty("created_at")
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(final String workflowId) {
        this.workflowId = workflowId;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public void setPromptTemplate(final String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(final String userName) {
        this.userName = userName;
    }

    public String getUseCase() {
        return useCase;
    }

    public void setUseCase(final String useCase) {
        this.useCase = useCase;
    }

    public String getPromptText() {
        return promptText;
    }

    public void setPromptText(final String promptText) {
        this.promptText = promptText;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(final String responseText) {
        this.responseText = responseText;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(final String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(final String model) {
        this.model = model;
    }

    public boolean isCacheHit() {
        return cacheHit;
    }

    public void setCacheHit(final boolean cacheHit) {
        this.cacheHit = cacheHit;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }
}
