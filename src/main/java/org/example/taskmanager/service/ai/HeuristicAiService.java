package org.example.taskmanager.service.ai;

import org.example.taskmanager.domain.Priority;
import org.example.taskmanager.dto.DeadlineEstimation;
import org.example.taskmanager.dto.ParsedTask;
import org.example.taskmanager.dto.PriorityPrediction;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Offline AI/ML engine based on keyword scoring and rule-based NLP. Requires no
 * external API keys, which keeps the application self-contained and demo-ready.
 * Enabled by default ({@code app.ai.provider=heuristic}) and also used as the
 * fallback engine when the OpenAI provider is unavailable.
 */
@Service("heuristicAiService")
public class HeuristicAiService implements AiService {

    private static final List<String> URGENT_TERMS = List.of(
            "urgent", "asap", "immediately", "critical", "emergency", "blocker",
            "right now", "today", "deadline", "overdue", "escalat");
    private static final List<String> HIGH_TERMS = List.of(
            "important", "high priority", "priority", "soon", "must", "need to",
            "tomorrow", "quick", "review", "client", "production", "bug");
    private static final List<String> LOW_TERMS = List.of(
            "someday", "eventually", "whenever", "low priority", "optional",
            "nice to have", "later", "backlog", "minor", "no rush");

    // Effort keywords used to size the estimated deadline.
    private static final List<String> LARGE_EFFORT = List.of(
            "design", "architect", "implement", "build", "migrate", "refactor",
            "research", "investigate", "integrate", "develop", "rewrite");
    private static final List<String> SMALL_EFFORT = List.of(
            "call", "email", "reply", "send", "review", "read", "check",
            "update", "fix typo", "rename", "ping", "schedule");

    private final NaturalLanguageDateParser dateParser;

    public HeuristicAiService(NaturalLanguageDateParser dateParser) {
        this.dateParser = dateParser;
    }

    @Override
    public PriorityPrediction predictPriority(String text) {
        String normalized = safeLower(text);
        int urgent = countMatches(normalized, URGENT_TERMS);
        int high = countMatches(normalized, HIGH_TERMS);
        int low = countMatches(normalized, LOW_TERMS);

        Priority priority;
        String reason;
        if (urgent > 0) {
            priority = Priority.URGENT;
            reason = "Detected urgency signals in the text (e.g. deadline/critical wording).";
        } else if (high > low) {
            priority = Priority.HIGH;
            reason = "Text emphasises importance or a near-term deadline.";
        } else if (low > 0 && high == 0) {
            priority = Priority.LOW;
            reason = "Text contains low-urgency wording (e.g. someday/optional).";
        } else {
            priority = Priority.MEDIUM;
            reason = "No strong urgency signals detected; defaulting to a balanced priority.";
        }

        int signalStrength = Math.max(urgent, Math.max(high, low));
        double confidence = Math.min(0.95, 0.55 + 0.15 * signalStrength);
        return new PriorityPrediction(priority, round(confidence), reason);
    }

    @Override
    public DeadlineEstimation estimateDeadline(String text) {
        // 1) If the text mentions an explicit deadline, honour it.
        Optional<Instant> explicit = dateParser.parse(text);
        if (explicit.isPresent()) {
            long hours = Math.max(1, Duration.between(Instant.now(), explicit.get()).toHours());
            return new DeadlineEstimation(hours, explicit.get(),
                    "Deadline extracted from an explicit time reference in the text.");
        }

        // 2) Otherwise estimate effort from wording + length and derive a due date.
        String normalized = safeLower(text);
        int words = normalized.isBlank() ? 0 : normalized.split("\\s+").length;

        long baseHours = 4; // default half working day
        String reason = "Baseline estimate for a standard task.";
        if (countMatches(normalized, LARGE_EFFORT) > 0) {
            baseHours = 24;
            reason = "Wording suggests a substantial, multi-step task.";
        } else if (countMatches(normalized, SMALL_EFFORT) > 0) {
            baseHours = 1;
            reason = "Wording suggests a quick, low-effort task.";
        }

        // Longer descriptions imply more scope.
        if (words > 40) {
            baseHours += 8;
            reason += " Description is detailed, so extra time was added.";
        }

        // Urgent tasks get a tighter suggested due date.
        if (predictPriority(text).priority() == Priority.URGENT) {
            baseHours = Math.min(baseHours, 8);
            reason += " Priority is urgent, so the suggested due date was pulled in.";
        }

        Instant suggested = Instant.now().plus(Duration.ofHours(baseHours));
        return new DeadlineEstimation(baseHours, suggested, reason);
    }

    @Override
    public ParsedTask parseTask(String message) {
        PriorityPrediction priority = predictPriority(message);
        Optional<Instant> dueDate = dateParser.parse(message);
        String title = deriveTitle(message);

        StringBuilder reasoning = new StringBuilder("Interpreted the message as a task titled \"")
                .append(title).append("\". ")
                .append("Assigned ").append(priority.priority()).append(" priority. ");
        dueDate.ifPresentOrElse(
                d -> reasoning.append("Detected a deadline of ").append(d).append("."),
                () -> reasoning.append("No explicit deadline detected."));

        return new ParsedTask(title, message.trim(), priority.priority(), dueDate.orElse(null), reasoning.toString());
    }

    /** Builds a concise title by stripping common assistant trigger phrases. */
    private String deriveTitle(String message) {
        String cleaned = message.trim()
                .replaceAll("(?i)^(please\\s+)?(remind me to|remember to|i need to|create a task to|"
                        + "add a task to|todo:?|task:?|create task|make a task to)\\s+", "")
                .trim();

        // Cut off at the first temporal/priority clause to keep the title short.
        cleaned = cleaned.replaceAll("(?i)\\s+(by|before|on|next|in \\d+|within|it'?s|its|because|,).*$", "").trim();

        if (cleaned.isBlank()) {
            cleaned = message.trim();
        }
        if (cleaned.length() > 150) {
            cleaned = cleaned.substring(0, 147) + "...";
        }
        // Capitalise the first letter for a tidy title.
        return cleaned.substring(0, 1).toUpperCase(Locale.ROOT) + cleaned.substring(1);
    }

    private int countMatches(String text, List<String> terms) {
        int count = 0;
        for (String term : terms) {
            int index = 0;
            while ((index = text.indexOf(term, index)) != -1) {
                count++;
                index += term.length();
            }
        }
        return count;
    }

    private String safeLower(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
