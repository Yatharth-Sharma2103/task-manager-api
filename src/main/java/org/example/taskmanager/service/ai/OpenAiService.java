package org.example.taskmanager.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.taskmanager.domain.Priority;
import org.example.taskmanager.dto.DeadlineEstimation;
import org.example.taskmanager.dto.ParsedTask;
import org.example.taskmanager.dto.PriorityPrediction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

/**
 * OpenAI-backed implementation of {@link AiService}. Enabled with
 * {@code app.ai.provider=openai} and a valid {@code app.ai.openai.api-key}.
 * Any failure (missing key, network/API error, unparsable response) transparently
 * falls back to the offline {@link HeuristicAiService} so the API never breaks.
 */
@Service
@Primary
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAiService implements AiService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);

    private final AiProperties properties;
    private final HeuristicAiService fallback;
    private final NaturalLanguageDateParser dateParser;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiService(AiProperties properties,
                         HeuristicAiService fallback,
                         NaturalLanguageDateParser dateParser) {
        this.properties = properties;
        this.fallback = fallback;
        this.dateParser = dateParser;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getOpenai().getBaseUrl())
                .build();
    }

    @Override
    public PriorityPrediction predictPriority(String text) {
        String system = "You are a task triage assistant. Classify the task priority as one of "
                + "LOW, MEDIUM, HIGH, URGENT. Respond ONLY as compact JSON: "
                + "{\"priority\":\"...\",\"confidence\":0.0,\"reasoning\":\"...\"}.";
        try {
            JsonNode json = chatJson(system, text);
            Priority priority = Priority.valueOf(json.path("priority").asText("MEDIUM").toUpperCase(Locale.ROOT));
            double confidence = json.path("confidence").asDouble(0.7);
            String reasoning = json.path("reasoning").asText("Classified by OpenAI.");
            return new PriorityPrediction(priority, confidence, reasoning);
        } catch (Exception ex) {
            log.warn("OpenAI priority prediction failed, falling back to heuristic engine: {}", ex.getMessage());
            return fallback.predictPriority(text);
        }
    }

    @Override
    public DeadlineEstimation estimateDeadline(String text) {
        String system = "You estimate how long a task will take. Respond ONLY as compact JSON: "
                + "{\"estimatedHours\":N,\"reasoning\":\"...\"}. estimatedHours is an integer number of hours.";
        try {
            JsonNode json = chatJson(system, text);
            long hours = Math.max(1, json.path("estimatedHours").asLong(4));
            String reasoning = json.path("reasoning").asText("Estimated by OpenAI.");
            // Respect an explicit deadline in the text when present.
            Instant suggested = dateParser.parse(text)
                    .orElse(Instant.now().plus(Duration.ofHours(hours)));
            return new DeadlineEstimation(hours, suggested, reasoning);
        } catch (Exception ex) {
            log.warn("OpenAI deadline estimation failed, falling back to heuristic engine: {}", ex.getMessage());
            return fallback.estimateDeadline(text);
        }
    }

    @Override
    public ParsedTask parseTask(String message) {
        String system = "You convert a natural-language request into a task. Respond ONLY as compact JSON: "
                + "{\"title\":\"...\",\"priority\":\"LOW|MEDIUM|HIGH|URGENT\",\"reasoning\":\"...\"}. "
                + "Keep the title short (max 150 chars).";
        try {
            JsonNode json = chatJson(system, message);
            String title = json.path("title").asText(message);
            Priority priority = Priority.valueOf(json.path("priority").asText("MEDIUM").toUpperCase(Locale.ROOT));
            String reasoning = json.path("reasoning").asText("Parsed by OpenAI.");
            Instant dueDate = dateParser.parse(message).orElse(null);
            return new ParsedTask(title, message.trim(), priority, dueDate, reasoning);
        } catch (Exception ex) {
            log.warn("OpenAI task parsing failed, falling back to heuristic engine: {}", ex.getMessage());
            return fallback.parseTask(message);
        }
    }

    private JsonNode chatJson(String systemPrompt, String userPrompt) throws Exception {
        String apiKey = properties.getOpenai().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured");
        }

        Map<String, Object> body = Map.of(
                "model", properties.getOpenai().getModel(),
                "temperature", 0.2,
                "response_format", Map.of("type", "json_object"),
                "messages", new Object[]{
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                });

        String response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(response);
        String content = root.path("choices").path(0).path("message").path("content").asText();
        return objectMapper.readTree(content);
    }
}
