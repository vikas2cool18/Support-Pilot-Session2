package com.acme.supportpilot.memory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Checkpoint D: chat memory = a repository (WHERE messages are stored)
 * + a strategy (WHICH messages are sent back to the model).
 *
 * <p>The repository is auto-configured by spring-ai-starter-model-chat-memory-repository-jdbc
 * and stores messages in PostgreSQL. MessageWindowChatMemory keeps only the last N messages.
 */
@Configuration
class MemoryConfig {

    @Bean
    ChatMemory chatMemory(ChatMemoryRepository repository,
                          @Value("${supportpilot.memory.max-messages:20}") int maxMessages) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(maxMessages)
                .build();
    }

}
