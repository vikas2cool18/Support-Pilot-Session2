package com.acme.supportpilot.triage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Checkpoint B: the structured result we want from the model.
 * Spring AI generates a JSON schema from this record; enums become fixed lists of allowed values.
 * Checkpoint C: Bean Validation annotations catch answers that parse but are still wrong.
 */
public record TicketTriage(
        @NotNull Category category,
        @NotNull Priority priority,
        @NotNull Sentiment sentiment,
        @NotBlank @Size(max = 300) String summary,
        @NotNull Team suggestedTeam) {

    /** Safe default used when the model's answer cannot be trusted (Checkpoint C). */
    static TicketTriage fallback(String ticket) {
        String text = ticket.strip().replaceAll("\\s+", " ");
        String summary = text.length() <= 120 ? text : text.substring(0, 117) + "...";
        return new TicketTriage(Category.OTHER, Priority.P3, Sentiment.NEUTRAL,
                "Needs manual triage: " + summary, Team.L1_SUPPORT);
    }

}
