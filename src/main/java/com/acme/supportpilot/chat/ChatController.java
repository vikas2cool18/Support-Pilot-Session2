package com.acme.supportpilot.chat;

import org.springframework.ai.chat.client.ChatClient;
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

}
