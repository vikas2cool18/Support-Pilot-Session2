package com.acme.supportpilot.triage;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.DefaultResourceLoader;

import com.acme.supportpilot.chat.FakeChatModel;

import static org.assertj.core.api.Assertions.assertThat;

/** Offline tests for Checkpoints B and C - no API key, no Ollama, no database. */
class TriageServiceTest {

    static final String VALID_JSON = """
            {"category":"BILLING","priority":"P2","sentiment":"NEGATIVE",
             "summary":"Customer was charged twice for the March invoice.","suggestedTeam":"BILLING_OPS"}
            """;

    static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private TriageService service(FakeChatModel model) {
        TriagePrompts prompts = new TriagePrompts(new DefaultResourceLoader(), "v1");
        return new TriageService(ChatClient.builder(model), prompts, validator, true);
    }

    @Test
    void validJsonBecomesATicketTriageRecord() {
        TriageResult result = service(new FakeChatModel(VALID_JSON)).triage("I was charged twice.");

        assertThat(result.fallback()).isFalse();
        assertThat(result.triage().category()).isEqualTo(Category.BILLING);
        assertThat(result.triage().priority()).isEqualTo(Priority.P2);
        assertThat(result.triage().suggestedTeam()).isEqualTo(Team.BILLING_OPS);
        assertThat(result.promptVersion()).isEqualTo("v1");
        assertThat(result.promptTokens()).isEqualTo(12);
        assertThat(result.completionTokens()).isEqualTo(34);
    }

    @Test
    void systemPromptHasAllowedValuesAndTicketStaysInUserMessage() {
        FakeChatModel model = new FakeChatModel(VALID_JSON);
        service(model).triage("I was charged twice.");

        String system = model.lastPrompt().getSystemMessage().getText();
        assertThat(system).contains("FEATURE_REQUEST", "ACCOUNT_SECURITY").doesNotContain("{categories}");
        assertThat(system).doesNotContain("charged twice");
        assertThat(model.lastPrompt().getUserMessage().getText()).contains("I was charged twice.");
    }

    @Test
    void temperatureIsZeroForClassification() {
        FakeChatModel model = new FakeChatModel(VALID_JSON);
        service(model).triage("I was charged twice.");

        assertThat(model.lastPrompt().getOptions()).isNotNull();
        assertThat(model.lastPrompt().getOptions().getTemperature()).isEqualTo(0.0);
    }

    @Test
    void textThatIsNotJsonFallsBackSafely() {
        TriageResult result = service(new FakeChatModel("Sorry, I cannot classify this."))
                .triage("Something went wrong with my account");

        assertThat(result.fallback()).isTrue();
        assertThat(result.triage().category()).isEqualTo(Category.OTHER);
        assertThat(result.triage().priority()).isEqualTo(Priority.P3);
        assertThat(result.triage().summary()).startsWith("Needs manual triage");
    }

    @Test
    void unknownEnumValueFallsBackSafely() {
        String invented = VALID_JSON.replace("\"BILLING\"", "\"SHIPPING\"");
        TriageResult result = service(new FakeChatModel(invented)).triage("Where is my parcel?");

        assertThat(result.fallback()).isTrue();
    }

    @Test
    void blankSummaryIsRejectedByBeanValidation() {
        String blankSummary = VALID_JSON.replace("Customer was charged twice for the March invoice.", " ");
        TriageResult result = service(new FakeChatModel(blankSummary)).triage("I was charged twice.");

        assertThat(result.fallback()).isTrue();
    }

    @Test
    void invalidAnswerIsRetriedWithFeedbackAndCanRecover() {
        FakeChatModel model = new FakeChatModel("not json at all", VALID_JSON);
        TriageResult result = service(model).triage("I was charged twice.");

        assertThat(result.fallback()).isFalse();
        assertThat(model.callCount()).isEqualTo(2);
    }

}
