package org.example.taskmanager.service;

import org.example.taskmanager.domain.Status;
import org.example.taskmanager.domain.Task;
import org.example.taskmanager.dto.AiAssistantResponse;
import org.example.taskmanager.dto.DeadlineEstimation;
import org.example.taskmanager.dto.ParsedTask;
import org.example.taskmanager.dto.TaskResponse;
import org.example.taskmanager.service.ai.AiService;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Turns a natural-language message into a persisted task using the AI engine.
 */
@Service
public class AiAssistantService {

    private final AiService aiService;
    private final TaskService taskService;

    public AiAssistantService(AiService aiService, TaskService taskService) {
        this.aiService = aiService;
        this.taskService = taskService;
    }

    public AiAssistantResponse createTaskFromMessage(String message) {
        ParsedTask parsed = aiService.parseTask(message);

        Task task = new Task();
        task.setTitle(parsed.title());
        task.setDescription(parsed.description());
        task.setPriority(parsed.priority());
        task.setStatus(Status.TODO);

        // If no deadline was extracted, let the AI estimate one.
        Instant dueDate = parsed.dueDate();
        if (dueDate == null) {
            DeadlineEstimation estimation = aiService.estimateDeadline(message);
            dueDate = estimation.suggestedDueDate();
        }
        task.setDueDate(dueDate);

        TaskResponse created = taskService.createFromEntity(task);

        String reply = "Created task \"" + created.title() + "\" with " + created.priority()
                + " priority" + (created.dueDate() != null ? ", due " + created.dueDate() : "")
                + ". " + parsed.reasoning();

        return new AiAssistantResponse(reply, created);
    }
}
