package net.mudpot.constructraos.orchestration.system.activities;

import jakarta.inject.Singleton;
import net.mudpot.constructraos.commons.orchestration.ai.activities.PromptActivities;
import net.mudpot.constructraos.commons.ai.model.PromptBundle;

import java.util.LinkedHashMap;
import java.util.Map;

@Singleton
public class PromptActivitiesImpl implements PromptActivities {
    private static final String HELLO_TEMPLATE = "starter_hello_v1";
    private static final String SYSTEM_PROMPT =
        "You are the first workflow a team sees in ConstructraOS. "
            + "Respond with a concise, friendly greeting that reflects the user's name and the business problem they described. "
            + "Keep the response to 2 short paragraphs. "
            + "Paragraph 1 should greet them and reflect their domain. "
            + "Paragraph 2 should suggest one practical next step for using this starter platform. "
            + "Do not include hidden reasoning, bullet lists, or markdown tables.";

    @Override
    public PromptBundle renderPrompt(final String templateName, final Map<String, Object> variables) {
        if (!HELLO_TEMPLATE.equals(sanitize(templateName))) {
            throw new RuntimeException("Unsupported prompt template: " + templateName);
        }
        final Map<String, Object> params = variables == null ? Map.of() : new LinkedHashMap<>(variables);
        final String name = sanitize(asString(params.get("name")));
        final String useCase = sanitize(asString(params.get("use_case")));

        final String userPrompt = String.format(
            "User name: %s\nBusiness problem/domain: %s\n\n"
                + "Write a short hello-world response that proves the workflow hit a real LLM. "
                + "Mention the platform foundations available: API boundary, Temporal orchestration, Postgres, Valkey caching, tracing, and OPA policy. "
                + "End with one concrete next step.",
            name,
            useCase
        );

        return new PromptBundle(HELLO_TEMPLATE, SYSTEM_PROMPT, userPrompt, Map.of(
            "name", name,
            "use_case", useCase
        ));
    }

    private static String asString(final Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String sanitize(final String value) {
        return value == null ? "" : value.trim();
    }
}
