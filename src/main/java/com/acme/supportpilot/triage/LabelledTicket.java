package com.acme.supportpilot.triage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Checkpoint E: one ticket with the answer a human expert gave.
 * A small labelled dataset like this is the seed of the evaluation suite in Session 5.
 */
public record LabelledTicket(String id, String ticket, Category category, Priority priority,
                             Sentiment sentiment, Team team) {

    /** Reads data/tickets-labelled.csv (header: id,ticket,category,priority,sentiment,team). */
    public static List<LabelledTicket> readCsv(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<LabelledTicket> tickets = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {          // skip the header
            String line = lines.get(i).strip();
            if (line.isEmpty()) {
                continue;
            }
            List<String> f = parseCsvLine(line);
            if (f.size() != 6) {
                throw new IllegalArgumentException("Line " + (i + 1) + " has " + f.size() + " fields, expected 6");
            }
            tickets.add(new LabelledTicket(f.get(0), f.get(1), Category.valueOf(f.get(2)),
                    Priority.valueOf(f.get(3)), Sentiment.valueOf(f.get(4)), Team.valueOf(f.get(5))));
        }
        return tickets;
    }

    /** Minimal CSV: commas separate fields; double quotes wrap fields; "" inside quotes is a quote. */
    static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                }
                else if (c == '"') {
                    quoted = false;
                }
                else {
                    current.append(c);
                }
            }
            else if (c == '"') {
                quoted = true;
            }
            else if (c == ',') {
                fields.add(current.toString().strip());
                current.setLength(0);
            }
            else {
                current.append(c);
            }
        }
        fields.add(current.toString().strip());
        return fields;
    }

}
