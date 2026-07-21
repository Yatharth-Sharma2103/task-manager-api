package org.example.taskmanager.dto;

import java.time.Instant;

/**
 * Result of the AI/NLP deadline-estimation engine.
 */
public record DeadlineEstimation(
        long estimatedHours,
        Instant suggestedDueDate,
        String reasoning
) {
}
