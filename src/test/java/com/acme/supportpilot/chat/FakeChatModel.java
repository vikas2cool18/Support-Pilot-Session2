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
 * A ChatModel that never calls the network. It returns scripted answers (the last one repeats)
 * and remembers every Prompt it received, so tests can check what was sent.
 * Session 1 introduced it; Session 2 made it public and scriptable for the triage and memory tests.
 */
public class FakeChatModel implements ChatModel {

    private final List<String> answers;

    private final List<Prompt> prompts = new ArrayList<>();

    public FakeChatModel(String... answers) {
        this.answers = List.of(answers);
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        this.prompts.add(prompt);
        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .model("fake-model")
                .usage(new DefaultUsage(12, 34))
                .build();
        return new ChatResponse(List.of(new Generation(new AssistantMessage(nextAnswer()))), metadata);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        this.prompts.add(prompt);
        // Split after each space to imitate token-by-token streaming: "Hello " + "there"
        List<String> pieces = Arrays.asList(nextAnswer().split("(?<= )"));
        return Flux.fromIterable(pieces)
                .map(piece -> new ChatResponse(List.of(new Generation(new AssistantMessage(piece)))));
    }

    private String nextAnswer() {
        int index = Math.min(this.prompts.size() - 1, this.answers.size() - 1);
        return this.answers.get(index);
    }

    public Prompt lastPrompt() {
        return this.prompts.get(this.prompts.size() - 1);
    }

    public int callCount() {
        return this.prompts.size();
    }

}
