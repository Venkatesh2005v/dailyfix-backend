package com.example.dailyfix.service;

import com.example.dailyfix.dto.request.MessageRequest;
import com.example.dailyfix.enums.Priority;
import com.example.dailyfix.model.Message;
import com.example.dailyfix.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final PriorityService priorityService;
    private final TaskService taskService;

    public MessageService(MessageRepository messageRepository, PriorityService priorityService, TaskService taskService) {
        this.messageRepository = messageRepository;
        this.priorityService = priorityService;
        this.taskService = taskService;
    }

    public void receiveMessage(MessageRequest request){
        Message message = new Message();
        message.setSenderEmail(request.getSenderEmail());
        message.setSourceType(request.getSourceType());
        message.setSubject(request.getSubject());
        message.setContent(request.getContent());

        message.setReceivedAt(LocalDateTime.now());
        message.setProcessed(false);

        messageRepository.save(message);

        processMessage(message);

    }

    public void processMessage(Message message){
        Priority priority = priorityService.calculatePriority(message);

        message.setPriority(priority);

        if(priority == Priority.HIGH || priority == Priority.MEDIUM){
            taskService.createTaskFromMessage(message);
            message.setProcessed(true);

        }
        else{
            message.setProcessed(false);
        }
        messageRepository.save(message);
    }

}
