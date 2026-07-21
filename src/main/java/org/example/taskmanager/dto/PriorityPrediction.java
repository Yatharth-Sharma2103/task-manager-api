package org.example.taskmanager.dto;

import org.example.taskmanager.domain.Priority;

/**
 * Result of the AI smart-prioritization engine.
 */
public record PriorityPrediction(
        Priority priority,
        double confidence,
        String reasoning
) {
}
