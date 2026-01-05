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
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final ActivityLogRepository activityLogRepository;

    public TaskService(TaskRepository taskRepository,
                       UserRepository userRepository,
                       ActivityLogService activityLogService,
                        ActivityLogRepository activityLogRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.activityLogService = activityLogService;
        this.activityLogRepository = activityLogRepository;
    }

    public void createTaskFromMessage(Message message) {
        validateMessage(message);

        // 🚨 AUTOMATIC ALERT: Triggers alert if priority is HIGH
        if (message.getPriority() == Priority.HIGH) {
            sendPriorityAlert(message);
        }

        Task task = buildTask(message);
        assignTask(task);
        setDueDate(task);

        taskRepository.save(task);

        User assignedUser = task.getAssignedTo();
        activityLogService.logActivity(
                task,
                assignedUser,
                ActionType.CREATED,
                "Task created from message automatically"
        );
    }

    private void sendPriorityAlert(Message message) {
        System.out.println("--------------------------------------------------");
        System.out.println("🚨 AUTOMATED HIGH PRIORITY ALERT");
        System.out.println("FROM: " + message.getSenderEmail());
        System.out.println("SUBJECT: " + message.getSubject());
        System.out.println("ACTION: Creating urgent task for Admin.");
        System.out.println("--------------------------------------------------");
    }

    private void validateMessage(Message message) {
        if (message == null) throw new IllegalArgumentException("Message cannot be null");
        if (message.isProcessed()) throw new IllegalStateException("Message already processed");
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
        if (admin == null) throw new RuntimeException("No admin user found");
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

    public List<Task> getAllTasks() { return taskRepository.findAll(); }

    public Optional<Task> getTaskById(Long id) { return taskRepository.findById(id); }

    public List<Task> getTasksByStatus(TaskStatus status) { return taskRepository.findByStatus(status); }

    public void completeTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        task.setStatus(TaskStatus.COMPLETED);
        taskRepository.save(task);
        activityLogService.logActivity(task, user, ActionType.COMPLETED, "Task marked as completed");
    }

    public void reassignTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        User newUser = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        task.setAssignedTo(newUser);
        taskRepository.save(task);
        activityLogService.logActivity(task, newUser, ActionType.REASSIGNED, "Task reassigned");
    }

    public @Nullable List<Task> getTasksByAssignedUserEmail(String userEmail) {
        return taskRepository.findByAssignedToEmail(userEmail);
    }


    @Transactional
    public void completeTaskByEmail(Long id, String email) {
        // 1. Find the task
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // 2. Find the user performing the action
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. Update the task status
        task.setStatus(TaskStatus.COMPLETED);
        taskRepository.save(task);

        // 4. LOG THE ACTIVITY
        ActivityLog log = new ActivityLog();
        log.setTask(task);
        log.setPerformedBy(user);
        log.setAction(ActionType.COMPLETED); // Using the ActionType enum
        log.setPerformedAt(LocalDateTime.now());
        log.setRemarks("Task completed via secure API endpoint");

        activityLogRepository.save(log);

    }
}