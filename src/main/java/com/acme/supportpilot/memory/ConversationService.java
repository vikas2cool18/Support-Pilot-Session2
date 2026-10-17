package com.acme.supportpilot.memory;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.acme.supportpilot.web.InvalidRequestException;

/**
 * Checkpoint D: multi-turn conversations.
 *
 * <p>The model is stateless. MessageChatMemoryAdvisor loads earlier messages for the
 * conversation ID, adds them to the prompt, and saves the new user and assistant messages.
 * This client has its own ChatClient because the memory advisor needs a conversation ID on
 * every call - the Session 1 /chat endpoints stay stateless.
 */
@Service
public class ConversationService {

    // UUIDs fit the 36-character conversation_id column of the JDBC repository.
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9-]{1,36}");

    private final ChatClient chat;

    private final ChatMemory chatMemory;

    public ConversationService(ChatClient.Builder builder, ChatMemory chatMemory,
                               @Value("classpath:prompts/system-v1.st") Resource systemPrompt) {
        this.chatMemory = chatMemory;
        this.chat = builder
                .defaultSystem(systemPrompt)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(), // runs first: adds history
                        new SimpleLoggerAdvisor())                              // logs the full prompt
                .build();
    }

    public String newConversation() {
        return UUID.randomUUID().toString();
    }

    public String send(String conversationId, String message) {
        checkId(conversationId);
        return this.chat.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }

    public List<ChatMessageView> history(String conversationId) {
        checkId(conversationId);
        return this.chatMemory.get(conversationId).stream()
                .map(m -> new ChatMessageView(m.getMessageType().name(), m.getText()))
                .toList();
    }

    public void clear(String conversationId) {
        checkId(conversationId);
        this.chatMemory.clear(conversationId);
    }

    private static void checkId(String conversationId) {
        if (conversationId == null || !VALID_ID.matcher(conversationId).matches()) {
            throw new InvalidRequestException("Invalid conversation ID - use the ID returned by POST /conversations.");
        }
    }

}
