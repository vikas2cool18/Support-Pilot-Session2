package com.acme.supportpilot.triage;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acme.supportpilot.web.InvalidRequestException;

@RestController
@RequestMapping("/triage")
public class TriageController {

    static final int MAX_TICKET_LENGTH = 4000;

    private final TriageService triageService;

    private final TriagePrompts prompts;

    public TriageController(TriageService triageService, TriagePrompts prompts) {
        this.triageService = triageService;
        this.prompts = prompts;
    }

    /** Checkpoint B/C: send the raw ticket text as the request body. */
    @PostMapping
    public TriageResult triage(@RequestBody String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw new InvalidRequestException("The ticket text is empty.");
        }
        if (ticket.length() > MAX_TICKET_LENGTH) {
            throw new InvalidRequestException("The ticket is longer than " + MAX_TICKET_LENGTH + " characters.");
        }
        return this.triageService.triage(ticket);
    }

    /** Checkpoint A: see the rendered system prompt for the active version. */
    @GetMapping("/prompt")
    public String prompt() {
        return "# prompt version: " + this.prompts.version() + "\n\n" + this.prompts.render();
    }

}
