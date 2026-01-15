package com.example.dailyfix.controller;

import com.example.dailyfix.enums.Priority;
import com.example.dailyfix.enums.TaskStatus;
import com.example.dailyfix.model.Message;
import com.example.dailyfix.model.Task;
import com.example.dailyfix.service.AIService;
import com.example.dailyfix.service.MessageService;
import com.example.dailyfix.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final MessageService messageService;
    private final TaskService taskService;
    private final AIService aiService;

    public DashboardController(MessageService messageService, TaskService taskService, AIService aiService) {
        this.messageService = messageService;
        this.taskService = taskService;
        this.aiService = aiService;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats(Authentication authentication) {
        String email = authentication.getName();
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

        List<Message> recentMessages = messageService.getMessagesByUserEmail(email).stream()
                .filter(m -> m.getReceivedAt() != null && m.getReceivedAt().isAfter(threeDaysAgo))
                .toList();


        String mailData = recentMessages.stream()
                .map(m -> String.format("From: %s | Subject: %s | Content: %s",
                        m.getSenderEmail() != null ? m.getSenderEmail() : "Unknown",
                        m.getSubject(),
                        m.getContent()))
                .collect(Collectors.joining("\n---\n"));

        List<Task> openTasks = taskService.getTasksByAssignedUserEmail(email).stream()
                .filter(t -> t.getStatus() == TaskStatus.OPEN)
                .toList();

        String taskData = openTasks.stream()
                .map(t -> t.getTitle() + " - " + t.getDescription())
                .collect(Collectors.joining(" | "));

        String urgentSummary = (recentMessages.isEmpty())
                ? "Recent correspondence is clear."
                : aiService.summarizeEmails(mailData);

        String normalSummary = (openTasks.isEmpty())
                ? "No active tasks currently pending."
                : aiService.summarizeTasks(taskData);

        return ResponseEntity.ok(Map.of(
                "totalMails", (long) recentMessages.size(),
                "urgentCount", recentMessages.stream().filter(m -> m.getPriority() == Priority.HIGH).count(),
                "taskCount", (long) openTasks.size(),
                "urgentSummary", urgentSummary,
                "normalSummary", normalSummary
        ));
    }
}