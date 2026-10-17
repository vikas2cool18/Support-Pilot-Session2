package com.acme.supportpilot.info;

import java.util.Arrays;
import java.util.Map;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Checkpoint E: shows which provider and model are active, so you can prove
 * that a profile switch changed the model without any code change.
 */
@RestController
public class ModelInfoController {

    private final ChatModel chatModel;

    private final Environment environment;

    public ModelInfoController(ChatModel chatModel, Environment environment) {
        this.chatModel = chatModel;
        this.environment = environment;
    }

    @GetMapping("/models")
    public Map<String, String> activeModel() {
        return Map.of(
                "provider", environment.getProperty("spring.ai.model.chat", "unknown"),
                "chatModelClass", chatModel.getClass().getSimpleName(),
                "model", String.valueOf(chatModel.getOptions().getModel()),
                "activeProfiles", Arrays.toString(environment.getActiveProfiles()));
    }

}
