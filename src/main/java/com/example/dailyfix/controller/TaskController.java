package com.example.dailyfix.controller;

import com.example.dailyfix.enums.TaskStatus;
import com.example.dailyfix.model.Task;
import com.example.dailyfix.service.TaskService;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Optional<Task>> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Task>> getTasksByStatus(@PathVariable TaskStatus status) {
        return ResponseEntity.ok(taskService.getTasksByStatus(status));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<String> completeTask(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        taskService.completeTask(id, userId);
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

