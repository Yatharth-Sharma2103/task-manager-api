package org.example.taskmanager.dto;

/**
 * Response from the AI assistant after turning a natural-language message
 * into a persisted task.
 */
public record AiAssistantResponse(
        String reply,
        TaskResponse task
) {
}
