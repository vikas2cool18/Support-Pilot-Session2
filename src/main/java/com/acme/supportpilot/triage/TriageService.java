package com.acme.supportpilot.triage;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Checkpoints B and C: turn free text into a validated TicketTriage record - or a safe fallback.
 */
@Service
public class TriageService {

    private static final Logger log = LoggerFactory.getLogger(TriageService.class);

    private final ChatClient chat;

    private final TriagePrompts prompts;

    private final Validator validator;

    private final boolean nativeStructuredOutput;

    public TriageService(ChatClient.Builder builder, TriagePrompts prompts, Validator validator,
                         @Value("${supportpilot.triage.native-structured-output:true}") boolean nativeStructuredOutput) {
        this.chat = builder
                // Classification needs consistency, not creativity.
                .defaultOptions(ChatOptions.builder().temperature(0.0))
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
        this.prompts = prompts;
        this.validator = validator;
        this.nativeStructuredOutput = nativeStructuredOutput;
    }

    public TriageResult triage(String ticket) {
        long start = System.nanoTime();
        try {
            ResponseEntity<ChatResponse, TicketTriage> result = this.chat.prompt()
                    // System prompt = template file + parameters. The ticket is NOT part of the template.
                    .system(s -> s.text(this.prompts.template()).params(this.prompts.params()))
                    // Untrusted customer text goes only into the user message.
                    .user(ticket)
                    .call()
                    .responseEntity(TicketTriage.class, spec -> {
                        // Check the JSON against the schema and retry with the error if it does not match.
                        spec.validateSchema();
                        if (this.nativeStructuredOutput) {
                            // Send the schema to the provider as an API-level constraint.
                            spec.useProviderStructuredOutput();
                        }
                    });

            TicketTriage triage = result.getEntity();
            List<String> problems = problemsWith(triage);
            if (!problems.isEmpty()) {
                log.warn("Rejected triage {} - problems: {}", triage, problems);
                return fallback(ticket, start);
            }
            Usage usage = (result.getResponse() != null) ? result.getResponse().getMetadata().getUsage() : null;
            return new TriageResult(triage, false, this.prompts.version(),
                    (usage != null) ? usage.getPromptTokens() : null,
                    (usage != null) ? usage.getCompletionTokens() : null,
                    elapsedMs(start));
        }
        catch (RuntimeException ex) {
            // Unparseable JSON, invalid enum value, provider error ... never a 500 for the caller.
            log.warn("Triage failed, using fallback: {}", ex.getMessage());
            return fallback(ticket, start);
        }
    }

    /** Checkpoint C: an answer can be valid JSON and still be wrong. */
    List<String> problemsWith(TicketTriage triage) {
        if (triage == null) {
            return List.of("no result");
        }
        Set<ConstraintViolation<TicketTriage>> violations = this.validator.validate(triage);
        return violations.stream()
                .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .toList();
    }

    private TriageResult fallback(String ticket, long start) {
        return new TriageResult(TicketTriage.fallback(ticket), true, this.prompts.version(), null, null,
                elapsedMs(start));
    }

    private static long elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

}
