package com.acme.supportpilot.memory;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.core.io.ByteArrayResource;

import com.acme.supportpilot.chat.FakeChatModel;
import com.acme.supportpilot.web.InvalidRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Offline tests for Checkpoint D, using an in-memory repository instead of PostgreSQL. */
class ConversationServiceTest {

    private final FakeChatModel model = new FakeChatModel("Hello there");

    private ConversationService service(int maxMessages) {
        ChatMemory memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(maxMessages)
                .build();
        return new ConversationService(ChatClient.builder(this.model), memory,
                new ByteArrayResource("You are SupportPilot.".getBytes(StandardCharsets.UTF_8)));
    }

    private List<String> textsSentInLastPrompt() {
        return this.model.lastPrompt().getInstructions().stream().map(Message::getText).toList();
    }

    @Test
    void secondMessageIncludesTheFirstExchange() {
        ConversationService service = service(20);
        String id = service.newConversation();

        service.send(id, "My name is Priya");
        service.send(id, "What is my name?");

        assertThat(textsSentInLastPrompt())
                .containsSubsequence("You are SupportPilot.", "My name is Priya", "Hello there", "What is my name?");
    }

    @Test
    void conversationsAreIsolated() {
        ConversationService service = service(20);
        String a = service.newConversation();
        String b = service.newConversation();

        service.send(a, "My order is ORD-10042");
        service.send(b, "Hi");

        assertThat(textsSentInLastPrompt()).doesNotContain("My order is ORD-10042");
    }

    @Test
    void historyShowsRolesInOrder() {
        ConversationService service = service(20);
        String id = service.newConversation();
        service.send(id, "Hi");

        assertThat(service.history(id)).extracting(ChatMessageView::role).containsExactly("USER", "ASSISTANT");
    }

    @Test
    void windowKeepsOnlyTheLastMessages() {
        ConversationService service = service(4);
        String id = service.newConversation();
        service.send(id, "one");
        service.send(id, "two");
        service.send(id, "three");

        List<ChatMessageView> history = service.history(id);
        assertThat(history).hasSize(4);
        assertThat(history.get(0).text()).isEqualTo("two");
    }

    @Test
    void clearForgetsTheConversation() {
        ConversationService service = service(20);
        String id = service.newConversation();
        service.send(id, "Hi");

        service.clear(id);

        assertThat(service.history(id)).isEmpty();
    }

    @Test
    void invalidConversationIdIsRejected() {
        assertThatThrownBy(() -> service(20).send("../../etc/passwd", "Hi"))
                .isInstanceOf(InvalidRequestException.class);
    }

}
