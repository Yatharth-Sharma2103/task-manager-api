package org.example.taskmanager.service;

import org.example.taskmanager.domain.Priority;
import org.example.taskmanager.domain.Status;
import org.example.taskmanager.domain.Task;
import org.example.taskmanager.domain.User;
import org.example.taskmanager.dto.PriorityPrediction;
import org.example.taskmanager.dto.TaskRequest;
import org.example.taskmanager.dto.TaskResponse;
import org.example.taskmanager.exception.ResourceNotFoundException;
import org.example.taskmanager.repository.TaskRepository;
import org.example.taskmanager.service.ai.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AiService aiService;

    public TaskService(TaskRepository taskRepository,
                       CurrentUserProvider currentUserProvider,
                       AiService aiService) {
        this.taskRepository = taskRepository;
        this.currentUserProvider = currentUserProvider;
        this.aiService = aiService;
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> list(Status status, Priority priority, Pageable pageable) {
        User owner = currentUserProvider.getCurrentUser();
        Page<Task> page;
        if (status != null) {
            page = taskRepository.findByOwnerAndStatus(owner, status, pageable);
        } else if (priority != null) {
            page = taskRepository.findByOwnerAndPriority(owner, priority, pageable);
        } else {
            page = taskRepository.findByOwner(owner, pageable);
        }
        return page.map(TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return TaskResponse.from(loadOwnedTask(id));
    }

    @Transactional
    public TaskResponse create(TaskRequest request) {
        User owner = currentUserProvider.getCurrentUser();
        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status() != null ? request.status() : Status.TODO);
        task.setDueDate(request.dueDate());

        // Smart Task Prioritization: predict priority when the client omits it.
        if (request.priority() != null) {
            task.setPriority(request.priority());
        } else {
            PriorityPrediction prediction = aiService.predictPriority(textFor(request));
            task.setPriority(prediction.priority());
            log.debug("AI predicted priority {} ({}) for task '{}'",
                    prediction.priority(), prediction.confidence(), request.title());
        }

        task.setOwner(owner);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest request) {
        Task task = loadOwnedTask(id);
        task.setTitle(request.title());
        task.setDescription(request.description());
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        task.setDueDate(request.dueDate());
        // A changed deadline should be eligible for a fresh reminder.
        task.setReminderSent(false);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateStatus(Long id, Status status) {
        Task task = loadOwnedTask(id);
        task.setStatus(status);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id) {
        Task task = loadOwnedTask(id);
        taskRepository.delete(task);
    }

    /** Persists a fully-formed task (used by the AI assistant). */
    @Transactional
    public TaskResponse createFromEntity(Task task) {
        task.setOwner(currentUserProvider.getCurrentUser());
        return TaskResponse.from(taskRepository.save(task));
    }

    private Task loadOwnedTask(Long id) {
        User owner = currentUserProvider.getCurrentUser();
        return taskRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));
    }

    private String textFor(TaskRequest request) {
        String description = request.description() == null ? "" : request.description();
        return (request.title() + ". " + description).trim();
    }
}
