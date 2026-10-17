package com.acme.supportpilot.memory;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.acme.supportpilot.web.InvalidRequestException;

@RestController
@RequestMapping("/conversations")
public class ConversationController {

    private final ConversationService conversations;

    public ConversationController(ConversationService conversations) {
        this.conversations = conversations;
    }

    public record ConversationCreated(String conversationId) {
    }

    public record ConversationReply(String conversationId, String answer) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationCreated create() {
        return new ConversationCreated(this.conversations.newConversation());
    }

    @PostMapping("/{id}/messages")
    public ConversationReply send(@PathVariable String id, @RequestBody String message) {
        if (message == null || message.isBlank()) {
            throw new InvalidRequestException("The message is empty.");
        }
        return new ConversationReply(id, this.conversations.send(id, message));
    }

    @GetMapping("/{id}/messages")
    public List<ChatMessageView> history(@PathVariable String id) {
        return this.conversations.history(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@PathVariable String id) {
        this.conversations.clear(id);
    }

}
