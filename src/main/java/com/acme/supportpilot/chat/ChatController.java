package com.acme.supportpilot.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatClient chat;

    public ChatController(ChatClient chat) {
        this.chat = chat;
    }

    /** Checkpoint B: the simplest possible call - question in, text out. */
    @GetMapping
    public String chat(@RequestParam String q) {
        return chat.prompt()
                .user(q)
                .call()
                .content();
    }

    /** Checkpoint C: same call, but keep the whole ChatResponse to read tokens and model. */
    @GetMapping("/meta")
    public ChatReply chatWithMetadata(@RequestParam String q) {
        long start = System.nanoTime();
        ChatResponse response = chat.prompt()
                .user(q)
                .call()
                .chatResponse();
        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        if (response == null) {
            throw new IllegalStateException("The model returned no response");
        }
        return ChatReply.from(response, latencyMs);
    }

}
