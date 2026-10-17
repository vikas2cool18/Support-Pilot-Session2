package com.acme.supportpilot.chat;

/**
 * Stretch goal: one answer produced at a given temperature.
 */
public record TemperatureRun(int run, double temperature, String answer) {
}
