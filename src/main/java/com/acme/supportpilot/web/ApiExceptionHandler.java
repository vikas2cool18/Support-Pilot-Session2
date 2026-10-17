package com.acme.supportpilot.web;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns model failures into a clear HTTP 502 with a hint, instead of a stack trace.
 * Production lens: users should never see raw provider errors.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail missingParameter(MissingServletRequestParameterException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Missing query parameter '" + ex.getParameterName() + "'. Example: /chat?q=How do I reset my password?");
    }

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail modelFailure(RuntimeException ex) {
        log.error("Model call failed", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, hint(ex));
        problem.setTitle("The AI model call failed");
        return problem;
    }

    /** Maps common Session 1 mistakes to a helpful message. Package-private for tests. */
    static String hint(Throwable ex) {
        String message = rootMessage(ex).toLowerCase(Locale.ROOT);
        if (message.contains("401") || message.contains("unauthorized") || message.contains("api key")
                || message.contains("api_key") || message.contains("authentication")) {
            return "Authentication failed. Check that OPENAI_API_KEY or ANTHROPIC_API_KEY is exported "
                    + "in the same terminal that runs the app.";
        }
        if (message.contains("connection refused") || message.contains("connectexception")
                || message.contains("11434")) {
            return "Could not reach the model server. If you use the 'local' profile, start Ollama "
                    + "(ollama serve) and check that it listens on port 11434.";
        }
        if (message.contains("temperature")) {
            return "This model does not accept a custom temperature. Use a non-reasoning model "
                    + "for the temperature experiment.";
        }
        if (message.contains("model") && (message.contains("not found") || message.contains("does not exist"))) {
            return "The configured model name was not found. Check the *_CHAT_MODEL variable, "
                    + "or run 'ollama pull <model>' for local models.";
        }
        if (message.contains("429") || message.contains("rate limit")) {
            return "Rate limit reached at the provider. Wait a moment, or switch to the 'local' profile.";
        }
        return "Unexpected error from the model provider. See the application log for details.";
    }

    private static String rootMessage(Throwable ex) {
        StringBuilder all = new StringBuilder();
        Throwable current = ex;
        while (current != null) {
            all.append(current.getClass().getSimpleName()).append(": ")
                    .append(current.getMessage()).append(' ');
            current = (current.getCause() == current) ? null : current.getCause();
        }
        return all.toString();
    }

}
