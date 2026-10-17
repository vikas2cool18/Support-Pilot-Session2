package com.acme.supportpilot.triage;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Checkpoint B: turn free text into a TicketTriage record.
 */
@Service
public class TriageService {

    private final ChatClient chat;

    private final TriagePrompts prompts;

    private final boolean nativeStructuredOutput;

    public TriageService(ChatClient.Builder builder, TriagePrompts prompts,
                         @Value("${supportpilot.triage.native-structured-output:true}") boolean nativeStructuredOutput) {
        this.chat = builder
                // Classification needs consistency, not creativity.
                .defaultOptions(ChatOptions.builder().temperature(0.0))
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
        this.prompts = prompts;
        this.nativeStructuredOutput = nativeStructuredOutput;
    }

    public TicketTriage triage(String ticket) {
        return this.chat.prompt()
                // System prompt = template file + parameters. The ticket is NOT part of the template.
                .system(s -> s.text(this.prompts.template()).params(this.prompts.params()))
                // Untrusted customer text goes only into the user message.
                .user(ticket)
                .call()
                .entity(TicketTriage.class, spec -> {
                    // Check the JSON against the schema and retry with the error if it does not match.
                    spec.validateSchema();
                    if (this.nativeStructuredOutput) {
                        // Send the schema to the provider as an API-level constraint.
                        spec.useProviderStructuredOutput();
                    }
                });
    }

}
