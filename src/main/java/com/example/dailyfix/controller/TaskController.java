package com.example.dailyfix.controller;

import com.example.dailyfix.model.Task;
import com.example.dailyfix.service.AIService;
import com.example.dailyfix.service.MessageService;
import com.example.dailyfix.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*", allowCredentials = "true")
public class TaskController {

    private final TaskService taskService;
    private final AIService aiService;
    private final MessageService messageService;

    public TaskController(TaskService taskService, AIService aiService, MessageService messageService) {
        this.taskService = taskService;
        this.aiService = aiService;
        this.messageService = messageService;
    }

    // --- 1. GET ALL TASKS ---
    @GetMapping
    public List<Task> getAllTasks() {
        return taskService.getAllTasks();
    }

    // --- 2. GET MY TASKS (Updated for Simultaneous Refresh) ---
    @GetMapping("/my-tasks")
    public List<Task> getMyTasks(
            @RequestParam(required = false) Long messageId,
            Authentication authentication
    ) {
        String email = getEmailFromAuth(authentication);

        // If a messageId is passed from React, filter the results
        if (messageId != null) {
            return taskService.getTasksByMessageIdAndEmail(messageId, email);
        }

        return taskService.getTasksByAssignedUserEmail(email);
    }

    // --- 3. COMPLETE TASK ---
    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeTask(@PathVariable Long id, Authentication authentication) {
        String email = getEmailFromAuth(authentication);
        try {
            taskService.completeTaskByEmail(id, email);
            return ResponseEntity.ok("Task completed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // --- 4. DISMISS TASK ---
    @PostMapping("/{id}/dismiss")
    public ResponseEntity<?> dismissTask(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            Authentication authentication
    ) {
        String email = getEmailFromAuth(authentication);
        String reason = payload.getOrDefault("reason", "No reason provided");

        try {
            taskService.dismissTask(id, email, reason);
            return ResponseEntity.ok("Task dismissed. Feedback recorded.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // 1. Endpoint to update status (Complete/Dismiss)
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        taskService.updateTaskStatus(id, newStatus);
        return ResponseEntity.ok().build();
    }

    // 2. Endpoint to generate the draft reply via Gemini
    @GetMapping("/{id}/generate-reply")
    public ResponseEntity<?> generateReply(@PathVariable Long id) {
        Task task = taskService.getTaskById(id);

        // Combine task title and content for Gemini context
        String context = "Task: " + task.getTitle() + " | Content: " + task.getDescription();
        String aiDraft = aiService.generateDraft(context);

        return ResponseEntity.ok(Map.of("reply", aiDraft));
    }

    @PostMapping("/{id}/send-reply")
    public ResponseEntity<?> sendReply(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String replyText = payload.get("replyText");

        // Fetch task and the associated message info
        Task task = taskService.getTaskById(id);
        String recipient = task.getSourceMessage().getSenderEmail();
        String subject = "DailyFix Update: " + task.getTitle();

        // Call the simplified service method
        messageService.sendNewEmail(recipient, subject, replyText);

        return ResponseEntity.ok(Map.of("status", "TRANSMITTED"));
    }

    private String getEmailFromAuth(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            return oauthToken.getPrincipal().getAttribute("email");
        }
        return authentication.getName();
    }


}