package com.acme.supportpilot.chat;

import java.util.List;
import java.util.stream.IntStream;

import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.http.MediaType;
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

    /** Checkpoint D: stream tokens to the client as Server-Sent Events. */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Token> stream(@RequestParam String q) {
        return chat.prompt()
                .user(q)
                .stream()
                .content()
                .map(Token::new);
    }

    /**
     * Stretch goal: ask the same question several times at one temperature.
     * Compare t=0.0 with t=1.0 to see sampling randomness for yourself.
     * Note: some reasoning models only accept their default temperature.
     */
    @GetMapping("/temperature")
    public List<TemperatureRun> temperature(@RequestParam String q,
                                            @RequestParam(defaultValue = "0.0") double t,
                                            @RequestParam(defaultValue = "5") int runs) {
        int safeRuns = Math.clamp(runs, 1, 10);
        return IntStream.rangeClosed(1, safeRuns)
                .mapToObj(i -> new TemperatureRun(i, t, chat.prompt()
                        .user(q)
                        .options(ChatOptions.builder().temperature(t))
                        .call()
                        .content()))
                .toList();
    }

}
