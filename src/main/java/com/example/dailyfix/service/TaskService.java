package com.example.dailyfix.service;

import com.example.dailyfix.enums.*;
import com.example.dailyfix.model.*;
import com.example.dailyfix.repository.*;
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
    private final MessageInteractionRepository interactionRepository;

    public TaskService(TaskRepository taskRepository,
                       UserRepository userRepository,
                       ActivityLogService activityLogService,
                       ActivityLogRepository activityLogRepository,
                       MessageInteractionRepository interactionRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.activityLogService = activityLogService;
        this.activityLogRepository = activityLogRepository;
        this.interactionRepository = interactionRepository;
    }

    // --- CORE LOGIC: Create Task ---
    public void createTaskFromMessage(Message message) {
        validateMessage(message);

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

    // --- INTERACTION LOGIC: Complete & Dismiss ---

    @Transactional
    public void completeTaskByEmail(Long id, String email) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        task.setStatus(TaskStatus.COMPLETED);
        taskRepository.save(task);

        // Audit Log
        activityLogService.logActivity(task, user, ActionType.COMPLETED, "Task completed via secure API");

        // Feedback Tracking
        MessageInteraction interaction = new MessageInteraction();
        interaction.setMessage(task.getSourceMessage());
        interaction.setUser(user);
        interaction.setAction(InteractionType.COMPLETED);
        interaction.setInteractedAt(LocalDateTime.now());
        interaction.setFeedbackNotes("Task completed normally");

        interactionRepository.save(interaction);
    }

    @Transactional
    public void dismissTask(Long taskId, String userEmail, String reason) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        task.setStatus(TaskStatus.CANCELLED);
        taskRepository.save(task);

        activityLogService.logActivity(task, user, ActionType.CANCELLED, "Task dismissed by user");

        MessageInteraction interaction = new MessageInteraction();
        interaction.setMessage(task.getSourceMessage());
        interaction.setUser(user);
        interaction.setAction(InteractionType.DISMISSED);
        interaction.setInteractedAt(LocalDateTime.now());
        interaction.setFeedbackNotes(reason);

        interactionRepository.save(interaction);
    }

    // --- RESTORED METHODS (Fixes "Cannot resolve method" errors) ---

    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    public void reassignTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        User newUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        task.setAssignedTo(newUser);
        taskRepository.save(task);

        activityLogService.logActivity(task, newUser, ActionType.REASSIGNED, "Task reassigned to new user");
    }

    public @Nullable List<Task> getTasksByAssignedUserEmail(String userEmail) {
        return taskRepository.findByAssignedToEmail(userEmail);
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public List<Task> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    public void completeTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        task.setStatus(TaskStatus.COMPLETED);
        taskRepository.save(task);
        activityLogService.logActivity(task, user, ActionType.COMPLETED, "Task marked as completed");
    }

    // --- PRIVATE HELPERS ---

    private void sendPriorityAlert(Message message) {
        System.out.println("--------------------------------------------------");
        System.out.println("🚨 AUTOMATED HIGH PRIORITY ALERT");
        System.out.println("FROM: " + message.getSenderEmail());
        System.out.println("SUBJECT: " + message.getSubject());
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
}