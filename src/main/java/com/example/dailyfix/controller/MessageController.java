package com.example.dailyfix.controller;

import com.example.dailyfix.enums.Priority;
import com.example.dailyfix.model.Message;
import com.example.dailyfix.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
     * MANUAL SYNC: Triggers the @Async fetch.
     * Use this to test the Gemini analysis immediately.
     */
    @PostMapping("/sync")
    public ResponseEntity<String> triggerSync(Authentication authentication) {
        messageService.fetchAndProcessGmail(authentication);
        return ResponseEntity.ok("Gemini AI sync started in the background! Refresh in 30 seconds.");
    }

    @GetMapping("/my-messages")
    public ResponseEntity<List<Message>> getMyMessages(Authentication authentication) {
        return ResponseEntity.ok(messageService.getMessagesByUserEmail(authentication.getName()));
    }

    /**
     * AI-FILTERED VIEW: Use this to see only "HIGH" priority items confirmed by Gemini.
     */
    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<Message>> getByPriority(
            Authentication authentication,
            @PathVariable Priority priority) {
        return ResponseEntity.ok(messageService.getMessagesByUserEmailAndPriority(authentication.getName(), priority));
    }

    @PostMapping("/{id}/reprocess")
    public ResponseEntity<String> reprocess(@PathVariable Long id) {
        messageService.reprocessMessage(id);
        return ResponseEntity.ok("Message re-analyzed by AI successfully.");
    }
}