package com.acme.supportpilot.triage;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/triage")
public class TriageController {

    private final TriagePrompts prompts;

    public TriageController(TriagePrompts prompts) {
        this.prompts = prompts;
    }

    /** Checkpoint A: see the rendered system prompt for the active version. */
    @GetMapping("/prompt")
    public String prompt() {
        return "# prompt version: " + this.prompts.version() + "\n\n" + this.prompts.render();
    }

}
