package com.acme.supportpilot.triage;

/**
 * Checkpoint B: the structured result we want from the model.
 * Spring AI generates a JSON schema from this record; enums become fixed lists of allowed values.
 */
public record TicketTriage(
        Category category,
        Priority priority,
        Sentiment sentiment,
        String summary,
        Team suggestedTeam) {
}
