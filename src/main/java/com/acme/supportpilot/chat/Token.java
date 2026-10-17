package com.acme.supportpilot.chat;

/**
 * Checkpoint D: one streamed piece of the answer.
 *
 * <p>Why a record instead of a plain String? Tokens often start with a space
 * (" world"). In Server-Sent Events the browser strips the first space after
 * "data:", so plain-text tokens lose their spaces. Sending JSON ({"text":" world"})
 * keeps them intact.
 */
public record Token(String text) {
}
