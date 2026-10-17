package com.acme.supportpilot.triage;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TriagePromptsTest {

    @Test
    void rendersEveryCategoryAndTeamFromTheEnums() {
        String prompt = new TriagePrompts(new DefaultResourceLoader(), "v1").render();

        for (Category c : Category.values()) {
            assertThat(prompt).contains("- " + c.name() + ": ");
        }
        for (Team t : Team.values()) {
            assertThat(prompt).contains("- " + t.name() + ": ");
        }
        assertThat(prompt).doesNotContain("{categories}", "{teams}");
    }

    @Test
    void version2AddsFewShotExamples() {
        String prompt = new TriagePrompts(new DefaultResourceLoader(), "v2").render();
        assertThat(prompt).contains("## Examples");
    }

    @Test
    void unknownVersionFailsFast() {
        assertThatThrownBy(() -> new TriagePrompts(new DefaultResourceLoader(), "v99"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("triage-system-v99.st");
    }

}
