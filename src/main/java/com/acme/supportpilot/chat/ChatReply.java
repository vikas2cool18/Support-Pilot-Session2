package com.acme.supportpilot.chat;

import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

/**
 * Checkpoint C: the answer plus the numbers an AI engineer cares about -
 * which model answered, how many tokens it cost and how long it took.
 */
public record ChatReply(
        String answer,
        String model,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        long latencyMs) {

    public static ChatReply from(ChatResponse response, long latencyMs) {
        ChatResponseMetadata metadata = response.getMetadata();
        Usage usage = metadata.getUsage();
        Generation result = response.getResult();
        String text = (result != null) ? result.getOutput().getText() : "";
        return new ChatReply(
                text,
                metadata.getModel(),
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                usage.getTotalTokens(),
                latencyMs);
    }

}
