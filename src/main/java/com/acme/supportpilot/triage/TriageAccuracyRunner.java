package com.acme.supportpilot.triage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Checkpoint E: measure triage accuracy against 20 hand-labelled tickets.
 *
 * <pre>
 * mvn spring-boot:run -Dspring-boot.run.profiles=local,accuracy
 * mvn spring-boot:run -Dspring-boot.run.profiles=local,accuracy -Dspring-boot.run.arguments=--supportpilot.triage.prompt-version=v2
 * </pre>
 */
@Component
@Profile("accuracy")
class TriageAccuracyRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TriageAccuracyRunner.class);

    private final TriageService triageService;

    private final TriagePrompts prompts;

    private final Path dataset;

    private final Path resultsDir;

    TriageAccuracyRunner(TriageService triageService, TriagePrompts prompts,
                         @Value("${supportpilot.accuracy.dataset:data/tickets-labelled.csv}") String dataset,
                         @Value("${supportpilot.homework.results-dir:results}") String resultsDir) {
        this.triageService = triageService;
        this.prompts = prompts;
        this.dataset = Path.of(dataset);
        this.resultsDir = Path.of(resultsDir);
    }

    @Override
    public void run(String... args) throws IOException {
        List<LabelledTicket> tickets = LabelledTicket.readCsv(this.dataset);
        Score score = new Score();
        List<String> rows = new ArrayList<>();
        rows.add("id,expected_category,actual_category,expected_priority,actual_priority,"
                + "expected_sentiment,actual_sentiment,expected_team,actual_team,fallback,latency_ms");
        List<String> mismatches = new ArrayList<>();

        for (LabelledTicket t : tickets) {
            TriageResult r = this.triageService.triage(t.ticket());
            TicketTriage a = r.triage();
            score.add(t, r);
            rows.add(String.join(",", t.id(), t.category().name(), a.category().name(), t.priority().name(),
                    a.priority().name(), t.sentiment().name(), a.sentiment().name(), t.team().name(),
                    a.suggestedTeam().name(), String.valueOf(r.fallback()), String.valueOf(r.latencyMs())));
            if (a.category() != t.category() || a.priority() != t.priority() || a.suggestedTeam() != t.team()) {
                mismatches.add(String.format("  %s  expected %s/%s/%s  got %s/%s/%s%s", t.id(), t.category(),
                        t.priority(), t.team(), a.category(), a.priority(), a.suggestedTeam(),
                        r.fallback() ? "  (fallback)" : ""));
            }
        }

        Files.createDirectories(this.resultsDir);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path out = this.resultsDir.resolve("triage-" + this.prompts.version() + "-" + stamp + ".csv");
        Files.write(out, rows, StandardCharsets.UTF_8);

        log.info("""

                ===== Triage accuracy - prompt {} - {} tickets =====
                  category   {}
                  priority   {}
                  sentiment  {}
                  team       {}
                  all four   {}
                  fallbacks  {}   avg latency {} ms
                Mismatches (category/priority/team):
                {}
                Details written to {}
                """, this.prompts.version(), tickets.size(), score.pct(score.category), score.pct(score.priority),
                score.pct(score.sentiment), score.pct(score.team), score.pct(score.allFour), score.fallbacks,
                score.avgLatency(), mismatches.isEmpty() ? "  none" : String.join("\n", mismatches),
                out.toAbsolutePath());
    }

    /** Running totals. */
    static final class Score {

        int total;

        int category;

        int priority;

        int sentiment;

        int team;

        int allFour;

        int fallbacks;

        long latency;

        void add(LabelledTicket t, TriageResult r) {
            TicketTriage a = r.triage();
            this.total++;
            boolean c = a.category() == t.category();
            boolean p = a.priority() == t.priority();
            boolean s = a.sentiment() == t.sentiment();
            boolean tm = a.suggestedTeam() == t.team();
            this.category += c ? 1 : 0;
            this.priority += p ? 1 : 0;
            this.sentiment += s ? 1 : 0;
            this.team += tm ? 1 : 0;
            this.allFour += (c && p && s && tm) ? 1 : 0;
            this.fallbacks += r.fallback() ? 1 : 0;
            this.latency += r.latencyMs();
        }

        String pct(int correct) {
            return String.format(Locale.ROOT, "%2d/%d  (%.0f%%)", correct, this.total,
                    (this.total == 0) ? 0.0 : 100.0 * correct / this.total);
        }

        long avgLatency() {
            return (this.total == 0) ? 0 : this.latency / this.total;
        }

    }

}
