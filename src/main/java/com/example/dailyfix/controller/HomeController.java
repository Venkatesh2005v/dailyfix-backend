package com.example.dailyfix.controller;

import com.example.dailyfix.service.MessageService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    private final MessageService messageService;

    public HomeController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/home")
    public String home(Authentication authentication) {
        messageService.fetchAndProcessGmail(authentication);
        return "Welcome " + authentication.getName() + "! Your high-priority emails have been processed into tasks.";
    }
}