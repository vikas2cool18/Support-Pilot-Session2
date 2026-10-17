package com.acme.supportpilot.triage;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/triage")
public class TriageController {

    private final TriageService triageService;

    private final TriagePrompts prompts;

    public TriageController(TriageService triageService, TriagePrompts prompts) {
        this.triageService = triageService;
        this.prompts = prompts;
    }

    /** Checkpoint B: send the raw ticket text as the request body. */
    @PostMapping
    public TicketTriage triage(@RequestBody String ticket) {
        return this.triageService.triage(ticket);
    }

    /** Checkpoint A: see the rendered system prompt for the active version. */
    @GetMapping("/prompt")
    public String prompt() {
        return "# prompt version: " + this.prompts.version() + "\n\n" + this.prompts.render();
    }

}
