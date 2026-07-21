package org.example.taskmanager.dto;

import org.example.taskmanager.domain.Priority;
import org.example.taskmanager.domain.Status;
import org.example.taskmanager.domain.Task;

import java.time.Instant;

public record TaskResponse(
        Long id,
        String title,
        String description,
        Priority priority,
        Status status,
        Instant dueDate,
        String owner,
        Instant createdAt,
        Instant updatedAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getStatus(),
                task.getDueDate(),
                task.getOwner() != null ? task.getOwner().getUsername() : null,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
