package org.example.taskmanager.dto;

import org.example.taskmanager.domain.Priority;

import java.time.Instant;

/**
 * Structured task intent extracted from a natural-language message by the AI engine.
 */
public record ParsedTask(
        String title,
        String description,
        Priority priority,
        Instant dueDate,
        String reasoning
) {
}
