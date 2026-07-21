package org.example.taskmanager.service;

import org.example.taskmanager.domain.Status;
import org.example.taskmanager.domain.Task;
import org.example.taskmanager.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Periodically scans for tasks whose deadline has passed (and are not yet done)
 * and "sends" a reminder. In a real deployment this would push an email/notification;
 * here it logs the reminder and marks the task so it is not reminded twice.
 *
 * <p>Demonstrates the optional {@code @Scheduled} deadline-reminder feature.</p>
 */
@Service
@ConditionalOnProperty(name = "app.reminders.enabled", havingValue = "true", matchIfMissing = true)
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final TaskRepository taskRepository;

    public ReminderService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /** Runs every 60 seconds (after an initial 10s delay). */
    @Scheduled(fixedDelayString = "${app.reminders.interval-ms:60000}", initialDelay = 10_000)
    @Transactional
    public void sendDueReminders() {
        List<Task> dueTasks = taskRepository
                .findByReminderSentFalseAndStatusNotAndDueDateBefore(Status.DONE, Instant.now());

        if (dueTasks.isEmpty()) {
            return;
        }

        for (Task task : dueTasks) {
            log.info("[REMINDER] Task '{}' (id={}) for user '{}' is due (deadline: {}). Current status: {}",
                    task.getTitle(),
                    task.getId(),
                    task.getOwner() != null ? task.getOwner().getUsername() : "unknown",
                    task.getDueDate(),
                    task.getStatus());
            task.setReminderSent(true);
        }
        taskRepository.saveAll(dueTasks);
    }
}
