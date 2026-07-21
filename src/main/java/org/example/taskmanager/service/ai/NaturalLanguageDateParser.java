package org.example.taskmanager.service.ai;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small rule-based NLP helper that extracts an absolute {@link Instant} deadline
 * from free-form English text (e.g. "by next Friday", "in 3 days", "tomorrow").
 */
@Component
public class NaturalLanguageDateParser {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final LocalTime END_OF_DAY = LocalTime.of(17, 0); // default deadline: 5pm

    private static final Pattern IN_N_UNITS =
            Pattern.compile("in\\s+(\\d+)\\s+(hour|hours|day|days|week|weeks|month|months)");
    private static final Pattern WITHIN_N_UNITS =
            Pattern.compile("within\\s+(\\d+)\\s+(hour|hours|day|days|week|weeks|month|months)");

    private static final Map<String, DayOfWeek> WEEKDAYS = Map.ofEntries(
            Map.entry("monday", DayOfWeek.MONDAY),
            Map.entry("tuesday", DayOfWeek.TUESDAY),
            Map.entry("wednesday", DayOfWeek.WEDNESDAY),
            Map.entry("thursday", DayOfWeek.THURSDAY),
            Map.entry("friday", DayOfWeek.FRIDAY),
            Map.entry("saturday", DayOfWeek.SATURDAY),
            Map.entry("sunday", DayOfWeek.SUNDAY)
    );

    /**
     * @return an extracted deadline, or empty when no temporal expression is found.
     */
    public Optional<Instant> parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Optional.empty();
        }
        String text = rawText.toLowerCase(Locale.ROOT);
        LocalDate today = LocalDate.now(ZONE);

        if (text.contains("tonight")) {
            return Optional.of(atTime(today, LocalTime.of(22, 0)));
        }
        if (text.contains("today")) {
            return Optional.of(atTime(today, END_OF_DAY));
        }
        if (text.contains("tomorrow")) {
            return Optional.of(atTime(today.plusDays(1), END_OF_DAY));
        }
        if (text.contains("next week")) {
            return Optional.of(atTime(today.plusWeeks(1), END_OF_DAY));
        }
        if (text.contains("next month")) {
            return Optional.of(atTime(today.plusMonths(1), END_OF_DAY));
        }
        if (text.contains("end of week") || text.contains("this week")) {
            return Optional.of(atTime(today.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)), END_OF_DAY));
        }

        Optional<Instant> relative = matchRelative(text, today);
        if (relative.isPresent()) {
            return relative;
        }

        return matchWeekday(text, today);
    }

    private Optional<Instant> matchRelative(String text, LocalDate today) {
        Matcher matcher = IN_N_UNITS.matcher(text);
        if (!matcher.find()) {
            matcher = WITHIN_N_UNITS.matcher(text);
            if (!matcher.find()) {
                return Optional.empty();
            }
        }
        int amount = Integer.parseInt(matcher.group(1));
        String unit = matcher.group(2);
        return switch (unit) {
            case "hour", "hours" -> Optional.of(Instant.now().plus(Duration.ofHours(amount)));
            case "day", "days" -> Optional.of(atTime(today.plusDays(amount), END_OF_DAY));
            case "week", "weeks" -> Optional.of(atTime(today.plusWeeks(amount), END_OF_DAY));
            case "month", "months" -> Optional.of(atTime(today.plusMonths(amount), END_OF_DAY));
            default -> Optional.empty();
        };
    }

    private Optional<Instant> matchWeekday(String text, LocalDate today) {
        boolean next = text.contains("next ");
        for (Map.Entry<String, DayOfWeek> entry : WEEKDAYS.entrySet()) {
            if (text.contains(entry.getKey())) {
                LocalDate target = today.with(TemporalAdjusters.next(entry.getValue()));
                if (next) {
                    target = target.plusWeeks(1);
                }
                return Optional.of(atTime(target, END_OF_DAY));
            }
        }
        return Optional.empty();
    }

    private Instant atTime(LocalDate date, LocalTime time) {
        return date.atTime(time).atZone(ZONE).toInstant();
    }
}
