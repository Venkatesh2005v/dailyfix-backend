package com.example.dailyfix.scheduler;

import com.example.dailyfix.model.User;
import com.example.dailyfix.repository.UserRepository;
import com.example.dailyfix.service.MessageService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class MailSyncScheduler {

    private final MessageService messageService;
    private final UserRepository userRepository;

    public MailSyncScheduler(MessageService messageService, UserRepository userRepository) {
        this.messageService = messageService;
        this.userRepository = userRepository;
    }

    @Scheduled(fixedRate = 120000) // 120 seconds
    public void runPeriodicSync() {
        System.out.println("--- Starting 2-Minute Background Intelligence Sync ---");
        List<User> users = userRepository.findAll();
        for (User user : users) {
            messageService.processWithToken(user.getEmail());
        }
    }
}