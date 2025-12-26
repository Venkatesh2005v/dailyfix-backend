package com.example.dailyfix.service;

import com.example.dailyfix.enums.ActionType;
import com.example.dailyfix.enums.Priority;
import com.example.dailyfix.enums.Role;
import com.example.dailyfix.enums.TaskStatus;
import com.example.dailyfix.model.ActivityLog;
import com.example.dailyfix.model.Message;
import com.example.dailyfix.model.Task;
import com.example.dailyfix.model.User;
import com.example.dailyfix.repository.ActivityLogRepository;
import com.example.dailyfix.repository.TaskRepository;
import com.example.dailyfix.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;

    public TaskService(TaskRepository taskRepository,
                       UserRepository userRepository,
                       ActivityLogRepository activityLogRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.activityLogRepository = activityLogRepository;
    }

    public void createTaskFromMessage(Message message) {

        validateMessage(message);

        Task task = buildTask(message);

        assignTask(task);

        setDueDate(task);

        taskRepository.save(task);

        logTaskCreation(task);
    }

    private void validateMessage(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        if (message.isProcessed()) {
            throw new IllegalStateException("Message already processed");
        }
    }

    private Task buildTask(Message message) {
        Task task = new Task();
        task.setTitle(message.getSubject());
        task.setDescription(message.getContent());
        task.setPriority(message.getPriority());
        task.setStatus(TaskStatus.OPEN);
        task.setSourceMessage(message);
        task.setCreatedAt(LocalDateTime.now());
        return task;
    }

    private void assignTask(Task task) {
        User admin = userRepository.findFirstByRole(Role.ADMIN);
        task.setAssignedTo(admin);
    }

    private void setDueDate(Task task) {
        LocalDateTime now = LocalDateTime.now();

        if (task.getPriority() == Priority.HIGH) {
            task.setDueDate(now.plusHours(4));
        } else if (task.getPriority() == Priority.MEDIUM) {
            task.setDueDate(now.plusDays(1));
        } else {
            task.setDueDate(now.plusDays(3));
        }
    }

    private void logTaskCreation(Task task) {
        ActivityLog log = new ActivityLog();
        log.setTask(task);
        log.setAction(ActionType.CREATED);
        log.setPerformedAt(LocalDateTime.now());
        log.setRemarks("Task created from message");
        activityLogRepository.save(log);
    }
}

