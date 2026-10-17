package com.acme.supportpilot.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Checkpoint B: one shared, pre-configured ChatClient.
 *
 * <p>ChatClient.Builder is auto-configured by Spring AI for whichever ChatModel is
 * active (OpenAI, Anthropic or Ollama). Our code never names a provider, which is
 * why switching models needs only a profile change.
 */
@Configuration
class ChatConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder,
                          @Value("classpath:prompts/system-v1.st") Resource systemPrompt) {
        return builder
                // System prompt in a file: reviewed and versioned like code.
                .defaultSystem(systemPrompt)
                // Logs every request and response at DEBUG level.
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

}
