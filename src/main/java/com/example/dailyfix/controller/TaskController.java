package com.example.dailyfix.controller;

import com.example.dailyfix.enums.TaskStatus;
import com.example.dailyfix.model.Task;
import com.example.dailyfix.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Added for security context
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * Fetch only tasks assigned to the CURRENT logged-in user.
     */
    @GetMapping("/my-tasks")
    public ResponseEntity<List<Task>> getMyTasks(Authentication authentication) {
        // authentication.getName() retrieves the email from the secure session
        String userEmail = authentication.getName();
        return ResponseEntity.ok(taskService.getTasksByAssignedUserEmail(userEmail));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Optional<Task>> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    /**
     * Complete a task using the session identity instead of a passed userId param.
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<String> completeTask(
            @PathVariable Long id,
            Authentication authentication
    ) {
        // We use the email from the session to find the user in the service
        taskService.completeTaskByEmail(id, authentication.getName());
        return ResponseEntity.ok("Task marked as completed");
    }

    @PostMapping("/{id}/reassign/{userId}")
    public ResponseEntity<String> reassignTask(
            @PathVariable Long id,
            @PathVariable Long userId) {
        taskService.reassignTask(id, userId);
        return ResponseEntity.ok("Task reassigned successfully");
    }
}