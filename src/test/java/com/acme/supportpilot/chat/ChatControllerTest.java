package com.acme.supportpilot.chat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fast, offline tests: no API key, no Ollama, no Spring context.
 * Run with: mvn test
 */
class ChatControllerTest {

    private FakeChatModel model;

    private ChatController controller;

    @BeforeEach
    void setUp() {
        this.model = new FakeChatModel("Hello there");
        ChatClient chat = ChatClient.builder(this.model)
                .defaultSystem("You are SupportPilot.")
                .build();
        this.controller = new ChatController(chat);
    }

    @Test
    void chatReturnsTheModelAnswer() {
        assertThat(this.controller.chat("Hi")).isEqualTo("Hello there");
    }

    @Test
    void chatSendsSystemAndUserMessages() {
        this.controller.chat("How do I reset my password?");

        assertThat(this.model.lastPrompt().getSystemMessage().getText()).isEqualTo("You are SupportPilot.");
        assertThat(this.model.lastPrompt().getUserMessage().getText()).isEqualTo("How do I reset my password?");
    }

    @Test
    void metaReportsModelTokensAndLatency() {
        ChatReply reply = this.controller.chatWithMetadata("Hi");

        assertThat(reply.answer()).isEqualTo("Hello there");
        assertThat(reply.model()).isEqualTo("fake-model");
        assertThat(reply.promptTokens()).isEqualTo(12);
        assertThat(reply.completionTokens()).isEqualTo(34);
        assertThat(reply.totalTokens()).isEqualTo(46);
        assertThat(reply.latencyMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void streamEmitsTokensInOrderAndKeepsSpaces() {
        StepVerifier.create(this.controller.stream("Hi").map(Token::text))
                .expectNext("Hello ")
                .expectNext("there")
                .verifyComplete();
    }

    @Test
    void temperatureIsPassedToTheModel() {
        List<TemperatureRun> runs = this.controller.temperature("Hi", 0.0, 3);

        assertThat(runs).hasSize(3);
        assertThat(this.model.callCount()).isEqualTo(3);
        assertThat(this.model.lastPrompt().getOptions()).isNotNull();
        assertThat(this.model.lastPrompt().getOptions().getTemperature()).isEqualTo(0.0);
    }

    @Test
    void temperatureRunsAreCappedAtTen() {
        assertThat(this.controller.temperature("Hi", 1.0, 50)).hasSize(10);
    }

}
