package com.acme.supportpilot.memory;

/** One stored message, as shown by GET /conversations/{id}/messages. */
public record ChatMessageView(String role, String text) {
}
