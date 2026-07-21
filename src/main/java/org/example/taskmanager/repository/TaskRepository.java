package org.example.taskmanager.repository;

import org.example.taskmanager.domain.Priority;
import org.example.taskmanager.domain.Status;
import org.example.taskmanager.domain.Task;
import org.example.taskmanager.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByOwner(User owner, Pageable pageable);

    Page<Task> findByOwnerAndStatus(User owner, Status status, Pageable pageable);

    Page<Task> findByOwnerAndPriority(User owner, Priority priority, Pageable pageable);

    Optional<Task> findByIdAndOwner(Long id, User owner);

    /** Tasks that are due, not yet done and have not had a reminder sent. */
    List<Task> findByReminderSentFalseAndStatusNotAndDueDateBefore(Status status, Instant before);
}
