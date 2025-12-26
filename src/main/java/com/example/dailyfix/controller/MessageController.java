package com.example.dailyfix.controller;

import com.example.dailyfix.dto.request.MessageRequest;
import com.example.dailyfix.enums.Priority;
import com.example.dailyfix.model.Message;
import com.example.dailyfix.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public ResponseEntity<String> receiveMessage(@RequestBody MessageRequest request){
        messageService.receiveMessage(request);

        return new ResponseEntity<>("Message received and processed successfully", HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Message>> getAllMessages() {
        return ResponseEntity.ok(messageService.getAllMessages());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Message> getMessageById(@PathVariable Long id) {
        return ResponseEntity.ok(messageService.getMessageById(id));
    }


    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<Message>> getMessagesByPriority(
            @PathVariable Priority priority) {
        return ResponseEntity.ok(messageService.getMessagesByPriority(priority));
    }


    @GetMapping("/unprocessed")
    public ResponseEntity<List<Message>> getUnprocessedMessages() {
        return ResponseEntity.ok(messageService.getUnprocessedMessages());
    }


    @PostMapping("/{id}/reprocess")
    public ResponseEntity<String> reprocessMessage(@PathVariable Long id) {
        messageService.reprocessMessage(id);
        return ResponseEntity.ok("Message reprocessed successfully");
    }
}
