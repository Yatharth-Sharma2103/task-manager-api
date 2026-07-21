package org.example.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.taskmanager.domain.Priority;
import org.example.taskmanager.domain.Status;

import java.time.Instant;

/**
 * Payload for creating/updating a task. Priority and status are optional on
 * create: when priority is omitted the AI engine predicts it from the description.
 */
public record TaskRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 2000) String description,
        Priority priority,
        Status status,
        Instant dueDate
) {
}
