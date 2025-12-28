package com.example.dailyfix.controller;

import com.example.dailyfix.dto.request.MessageRequest;
import com.example.dailyfix.enums.Priority;
import com.example.dailyfix.model.Message;
import com.example.dailyfix.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Added for security context
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * Gets all messages belonging to the CURRENT logged-in user.
     */
    @GetMapping("/my-messages")
    public ResponseEntity<List<Message>> getMyMessages(Authentication authentication) {
        // authentication.getName() retrieves the email from the Google OAuth2 session
        String userEmail = authentication.getName();
        return ResponseEntity.ok(messageService.getMessagesByUserEmail(userEmail));
    }

    /**
     * Filters the current user's messages by priority.
     */
    @GetMapping("/my-messages/priority/{priority}")
    public ResponseEntity<List<Message>> getMyMessagesByPriority(
            Authentication authentication,
            @PathVariable Priority priority) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(messageService.getMessagesByUserEmailAndPriority(userEmail, priority));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Message> getMessageById(Authentication authentication, @PathVariable Long id) {
        // Optional: Add logic in service to verify the message owner matches the authentication
        return ResponseEntity.ok(messageService.getMessageById(id));
    }

    @PostMapping("/{id}/reprocess")
    public ResponseEntity<String> reprocessMessage(@PathVariable Long id) {
        messageService.reprocessMessage(id);
        return ResponseEntity.ok("Message reprocessed successfully");
    }

    // Note: receiveMessage via PathVariable Long userId is less secure for public APIs.
    // It is better to use the authenticated session whenever possible.
}