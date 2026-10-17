package com.acme.supportpilot.web;

import java.net.ConnectException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    @Test
    void missingApiKeyGivesAuthenticationHint() {
        String hint = ApiExceptionHandler.hint(new RuntimeException("HTTP 401 Unauthorized"));
        assertThat(hint).contains("API_KEY");
    }

    @Test
    void ollamaNotRunningGivesConnectionHint() {
        RuntimeException ex = new RuntimeException("I/O error",
                new ConnectException("Connection refused: localhost/127.0.0.1:11434"));
        assertThat(ApiExceptionHandler.hint(ex)).contains("ollama serve");
    }

    @Test
    void unknownErrorGivesGenericHint() {
        assertThat(ApiExceptionHandler.hint(new RuntimeException("boom"))).contains("application log");
    }

}
