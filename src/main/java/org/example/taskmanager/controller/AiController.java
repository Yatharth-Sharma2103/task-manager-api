package org.example.taskmanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.taskmanager.dto.AiAnalyzeRequest;
import org.example.taskmanager.dto.AiAssistantResponse;
import org.example.taskmanager.dto.AiCommandRequest;
import org.example.taskmanager.dto.DeadlineEstimation;
import org.example.taskmanager.dto.PriorityPrediction;
import org.example.taskmanager.service.AiAssistantService;
import org.example.taskmanager.service.ai.AiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI/ML powered endpoints: smart prioritization, deadline estimation and a
 * natural-language task assistant.
 */
@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI", description = "AI-powered task prioritization, deadline estimation and assistant")
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final AiService aiService;
    private final AiAssistantService assistantService;

    public AiController(AiService aiService, AiAssistantService assistantService) {
        this.aiService = aiService;
        this.assistantService = assistantService;
    }

    @PostMapping("/prioritize")
    @Operation(summary = "Predict a task's priority from its description (Smart Task Prioritization)")
    public PriorityPrediction prioritize(@Valid @RequestBody AiAnalyzeRequest request) {
        return aiService.predictPriority(request.text());
    }

    @PostMapping("/estimate-deadline")
    @Operation(summary = "Estimate completion time and suggest a due date (Deadline Estimation)")
    public DeadlineEstimation estimateDeadline(@Valid @RequestBody AiAnalyzeRequest request) {
        return aiService.estimateDeadline(request.text());
    }

    @PostMapping("/assistant")
    @Operation(summary = "Create a task from a natural-language message (AI Assistant)")
    public ResponseEntity<AiAssistantResponse> assistant(@Valid @RequestBody AiCommandRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assistantService.createTaskFromMessage(request.message()));
    }
}
