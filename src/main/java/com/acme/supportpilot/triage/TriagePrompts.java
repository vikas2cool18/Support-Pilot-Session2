package com.acme.supportpilot.triage;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * Checkpoint A: prompts are versioned files, not Java strings.
 * The active version comes from configuration (supportpilot.triage.prompt-version), so you can
 * compare v1 and v2 without touching code. The allowed values are rendered from the enums,
 * so the prompt and the Java types can never disagree.
 */
@Component
public class TriagePrompts {

    private final String version;

    private final Resource template;

    public TriagePrompts(ResourceLoader resourceLoader,
                         @Value("${supportpilot.triage.prompt-version:v1}") String version) {
        this.version = version;
        this.template = resourceLoader.getResource("classpath:prompts/triage-system-" + version + ".st");
        if (!this.template.exists()) {
            throw new IllegalStateException("Prompt file not found: prompts/triage-system-" + version + ".st");
        }
    }

    public String version() {
        return this.version;
    }

    public Resource template() {
        return this.template;
    }

    /** Values for the {categories} and {teams} placeholders. */
    public Map<String, Object> params() {
        String categories = Arrays.stream(Category.values())
                .map(c -> "- " + c.name() + ": " + c.description())
                .collect(Collectors.joining("\n"));
        String teams = Arrays.stream(Team.values())
                .map(t -> "- " + t.name() + ": " + t.description())
                .collect(Collectors.joining("\n"));
        return Map.of("categories", categories, "teams", teams);
    }

    /** The system prompt exactly as the model will see it. */
    public String render() {
        return PromptTemplate.builder()
                .resource(this.template)
                .variables(params())
                .build()
                .render();
    }

}
