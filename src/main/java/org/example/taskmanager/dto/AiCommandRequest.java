package org.example.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Natural-language instruction for the AI assistant, e.g.
 * "Remind me to submit the quarterly tax report by next Friday, it's urgent".
 */
public record AiCommandRequest(
        @NotBlank @Size(max = 2000) String message
) {
}
