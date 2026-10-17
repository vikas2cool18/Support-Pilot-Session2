package com.acme.supportpilot.triage;

/**
 * Checkpoint C: what the API returns - the triage plus how we got it.
 * fallback=true means the model's answer was rejected and a safe default was used.
 */
public record TriageResult(
        TicketTriage triage,
        boolean fallback,
        String promptVersion,
        Integer promptTokens,
        Integer completionTokens,
        long latencyMs) {
}
