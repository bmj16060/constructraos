package net.mudpot.constructraos.commons.ai.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class PromptBundle {
    private String template;
    private String systemPrompt;
    private String userPrompt;
    private Map<String, Object> params = new LinkedHashMap<>();

    public PromptBundle() {
    }

    public PromptBundle(
        final String template,
        final String systemPrompt,
        final String userPrompt,
        final Map<String, Object> params
    ) {
        this.template = template;
        this.systemPrompt = systemPrompt;
        this.userPrompt = userPrompt;
        this.params = params == null ? new LinkedHashMap<>() : new LinkedHashMap<>(params);
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(final String template) {
        this.template = template;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(final String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public void setUserPrompt(final String userPrompt) {
        this.userPrompt = userPrompt;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(final Map<String, Object> params) {
        this.params = params == null ? new LinkedHashMap<>() : new LinkedHashMap<>(params);
    }
}
