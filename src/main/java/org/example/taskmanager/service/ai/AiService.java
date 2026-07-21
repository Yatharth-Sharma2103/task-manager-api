package org.example.taskmanager.service.ai;

import org.example.taskmanager.dto.DeadlineEstimation;
import org.example.taskmanager.dto.ParsedTask;
import org.example.taskmanager.dto.PriorityPrediction;

/**
 * Abstraction over the AI/ML capabilities of the application. Two implementations
 * are provided: an offline heuristic/NLP engine (default) and an OpenAI-backed
 * engine (enabled via {@code app.ai.provider=openai}).
 */
public interface AiService {

    /** Predicts a task priority from free-form text (Smart Task Prioritization). */
    PriorityPrediction predictPriority(String text);

    /** Estimates completion effort and suggests a due date (Deadline Estimation). */
    DeadlineEstimation estimateDeadline(String text);

    /** Converts a natural-language instruction into a structured task (AI Assistant). */
    ParsedTask parseTask(String message);
}
