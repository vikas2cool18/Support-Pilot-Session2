package com.acme.supportpilot.chat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import reactor.core.publisher.Flux;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * A ChatModel that never calls the network. It returns a fixed answer and
 * remembers every Prompt it received, so tests can check what was sent.
 * This is the first step towards testing AI code (Session 5).
 */
class FakeChatModel implements ChatModel {

    private final String answer;

    private final List<Prompt> prompts = new ArrayList<>();

    FakeChatModel(String answer) {
        this.answer = answer;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        this.prompts.add(prompt);
        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .model("fake-model")
                .usage(new DefaultUsage(12, 34))
                .build();
        return new ChatResponse(List.of(new Generation(new AssistantMessage(this.answer))), metadata);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        this.prompts.add(prompt);
        // Split after each space to imitate token-by-token streaming: "Hello " + "there"
        List<String> pieces = Arrays.asList(this.answer.split("(?<= )"));
        return Flux.fromIterable(pieces)
                .map(piece -> new ChatResponse(List.of(new Generation(new AssistantMessage(piece)))));
    }

    Prompt lastPrompt() {
        return this.prompts.get(this.prompts.size() - 1);
    }

    int callCount() {
        return this.prompts.size();
    }

}
