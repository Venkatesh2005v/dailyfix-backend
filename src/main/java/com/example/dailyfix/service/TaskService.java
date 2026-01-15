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
    private final MessageInteractionRepository interactionRepository;

    public TaskService(TaskRepository taskRepository,
                       UserRepository userRepository,
                       ActivityLogService activityLogService,
                       MessageInteractionRepository interactionRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.activityLogService = activityLogService;
        this.interactionRepository = interactionRepository;
    }

    // --- NEW: SIMULTANEOUS REFRESH LOGIC ---

    /**
     * Fetches tasks specifically linked to a selected message and user.
     * This ensures the "Active Directives" update when you switch emails.
     */
    public List<Task> getTasksByMessageIdAndEmail(Long messageId, String userEmail) {
        return taskRepository.findBySourceMessageIdAndAssignedToEmail(messageId, userEmail);
    }

    // --- CORE LOGIC: Create Task ---

    @Transactional
    public void createTaskFromMessage(Message message) {
        validateMessage(message);
        Task task = buildTask(message);
        task.setAssignedTo(message.getUser()); // Fixed: linked to email recipient
        setDueDate(task);

        if (task.getPriority() == Priority.HIGH) {
            sendPriorityAlert(message);
        }

        taskRepository.save(task);

        activityLogService.logActivity(
                task,
                message.getUser(),
                ActionType.CREATED,
                "AI generated directive from inbound intelligence"
        );
    }

    // --- DASHBOARD INTERACTIONS ---

    @Transactional
    public void completeTaskByEmail(Long id, String email) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        task.setStatus(TaskStatus.COMPLETED);
        taskRepository.save(task);

        activityLogService.logActivity(task, user, ActionType.COMPLETED, "Task completed via secure API");

        // Log interaction for AI feedback loop
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

    // --- DATA RETRIEVAL ---

    public List<Task> getAll() {
        return taskRepository.findAll();
    }

    public @Nullable List<Task> getTasksByAssignedUserEmail(String userEmail) {
        return taskRepository.findByAssignedToEmail(userEmail);
    }

    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    // --- PRIVATE HELPERS ---

    private Task buildTask(Message message) {
        Task task = new Task();
        task.setTitle(message.getSubject());
        task.setDescription(message.getContent());
        task.setPriority(message.getPriority());
        task.setStatus(TaskStatus.OPEN);
        task.setSourceMessage(message); // Links task to the specific email
        task.setCreatedAt(LocalDateTime.now());
        return task;
    }

    private void setDueDate(Task task) {
        LocalDateTime now = LocalDateTime.now();
        if (task.getPriority() == null) {
            task.setDueDate(now.plusDays(3));
            return;
        }
        switch (task.getPriority()) {
            case HIGH -> task.setDueDate(now.plusHours(4));
            case MEDIUM -> task.setDueDate(now.plusDays(1));
            default -> task.setDueDate(now.plusDays(3));
        }
    }

    private void validateMessage(Message message) {
        if (message == null) throw new IllegalArgumentException("Message cannot be null");
    }

    private void sendPriorityAlert(Message message) {
        System.out.println("🚨 HIGH PRIORITY ALERT: " + message.getSubject());
    }
}