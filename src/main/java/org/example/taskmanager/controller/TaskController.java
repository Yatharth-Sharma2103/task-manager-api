package org.example.taskmanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.taskmanager.domain.Priority;
import org.example.taskmanager.domain.Status;
import org.example.taskmanager.dto.TaskRequest;
import org.example.taskmanager.dto.TaskResponse;
import org.example.taskmanager.service.TaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD endpoints for tasks. All operations are scoped to the authenticated user,
 * so a user can only ever see or modify their own tasks.
 */
@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "CRUD operations for user-scoped tasks")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @Operation(summary = "List the current user's tasks (optionally filtered by status/priority)")
    public Page<TaskResponse> list(@RequestParam(required = false) Status status,
                                   @RequestParam(required = false) Priority priority,
                                   @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return taskService.list(status, priority, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single task by id")
    public TaskResponse get(@PathVariable Long id) {
        return taskService.get(id);
    }

    @PostMapping
    @Operation(summary = "Create a task (priority is AI-predicted when omitted)")
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return taskService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update only the status of a task")
    public TaskResponse updateStatus(@PathVariable Long id, @RequestParam Status status) {
        return taskService.updateStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
