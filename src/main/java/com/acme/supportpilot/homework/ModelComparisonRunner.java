package com.acme.supportpilot.homework;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.acme.supportpilot.chat.ChatReply;

/**
 * Homework helper: asks every question in homework/questions.txt to the active
 * model and writes a CSV with latency and token counts. You add the quality score.
 *
 * <p>Run once per model, for example:
 * <pre>
 * mvn spring-boot:run -Dspring-boot.run.profiles=local,compare
 * mvn spring-boot:run -Dspring-boot.run.profiles=compare
 * </pre>
 */
@Component
@Profile("compare")
class ModelComparisonRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ModelComparisonRunner.class);

    private final ChatClient chat;

    private final ChatModel chatModel;

    private final Path questionsFile;

    private final Path resultsDir;

    ModelComparisonRunner(ChatClient chat, ChatModel chatModel,
                          @Value("${supportpilot.homework.questions}") String questionsFile,
                          @Value("${supportpilot.homework.results-dir}") String resultsDir) {
        this.chat = chat;
        this.chatModel = chatModel;
        this.questionsFile = Path.of(questionsFile);
        this.resultsDir = Path.of(resultsDir);
    }

    @Override
    public void run(String... args) throws IOException {
        List<String> questions = Files.readAllLines(questionsFile, StandardCharsets.UTF_8).stream()
                .map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .toList();

        String modelName = String.valueOf(chatModel.getOptions().getModel());
        List<String> rows = new ArrayList<>();
        rows.add("question_no,question,model,latency_ms,prompt_tokens,completion_tokens,answer,quality_1_to_5");

        for (int i = 0; i < questions.size(); i++) {
            String question = questions.get(i);
            log.info("[{}/{}] {}", i + 1, questions.size(), question);
            long start = System.nanoTime();
            ChatResponse response = chat.prompt().user(question).call().chatResponse();
            long latencyMs = (System.nanoTime() - start) / 1_000_000;
            if (response == null) {
                continue;
            }
            ChatReply reply = ChatReply.from(response, latencyMs);
            rows.add(String.join(",",
                    String.valueOf(i + 1), csv(question), csv(reply.model()), String.valueOf(reply.latencyMs()),
                    String.valueOf(reply.promptTokens()), String.valueOf(reply.completionTokens()),
                    csv(reply.answer()), ""));
        }

        Files.createDirectories(resultsDir);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path out = resultsDir.resolve(safe(modelName) + "-" + stamp + ".csv");
        Files.write(out, rows, StandardCharsets.UTF_8);
        log.info("Wrote {} answers to {} - now add your 1-5 quality score in the last column.",
                rows.size() - 1, out.toAbsolutePath());
    }

    private static String csv(String value) {
        String v = (value == null) ? "" : value.replace("\r", " ").replace("\n", " ");
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private static String safe(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.-]+", "-");
    }

}
