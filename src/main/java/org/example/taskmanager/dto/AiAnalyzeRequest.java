package org.example.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Free-form task text used by the AI analysis endpoints
 * (priority prediction and deadline estimation).
 */
public record AiAnalyzeRequest(
        @NotBlank @Size(max = 2000) String text
) {
}
