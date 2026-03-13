package net.mudpot.constructraos.commons.ai.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class LlmResponse {
    private String provider;
    private String model;
    private String text;
    private Map<String, Object> usage = new LinkedHashMap<>();
    private Map<String, Object> cost = new LinkedHashMap<>();
    private Map<String, Object> cache = new LinkedHashMap<>();

    public LlmResponse() {
    }

    public LlmResponse(
        final String provider,
        final String model,
        final String text,
        final Map<String, Object> usage,
        final Map<String, Object> cost,
        final Map<String, Object> cache
    ) {
        this.provider = provider;
        this.model = model;
        this.text = text;
        this.usage = usage == null ? new LinkedHashMap<>() : new LinkedHashMap<>(usage);
        this.cost = cost == null ? new LinkedHashMap<>() : new LinkedHashMap<>(cost);
        this.cache = cache == null ? new LinkedHashMap<>() : new LinkedHashMap<>(cache);
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

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public Map<String, Object> getUsage() {
        return usage;
    }

    public void setUsage(final Map<String, Object> usage) {
        this.usage = usage == null ? new LinkedHashMap<>() : new LinkedHashMap<>(usage);
    }

    public Map<String, Object> getCost() {
        return cost;
    }

    public void setCost(final Map<String, Object> cost) {
        this.cost = cost == null ? new LinkedHashMap<>() : new LinkedHashMap<>(cost);
    }

    public Map<String, Object> getCache() {
        return cache;
    }

    public void setCache(final Map<String, Object> cache) {
        this.cache = cache == null ? new LinkedHashMap<>() : new LinkedHashMap<>(cache);
    }
}
