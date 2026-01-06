package com.example.dailyfix.controller;

import com.example.dailyfix.model.Task;
import com.example.dailyfix.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // --- 1. GET ALL TASKS ---
    @GetMapping
    public List<Task> getAllTasks() {
        return taskService.getAllTasks();
    }

    // --- 2. GET MY TASKS ---
    @GetMapping("/my-tasks")
    public List<Task> getMyTasks(Authentication authentication) {
        String email = getEmailFromAuth(authentication); // Now calls the helper below
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

    // --- 4. DISMISS TASK (New) ---
    @PostMapping("/{id}/dismiss")
    public ResponseEntity<?> dismissTask(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload, // Expects JSON: {"reason": "Not Urgent"}
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

    // --- PRIVATE HELPER METHOD ---
    // (Must be inside the "public class TaskController" brackets)
    private String getEmailFromAuth(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            return oauthToken.getPrincipal().getAttribute("email");
        }
        return authentication.getName();
    }
}