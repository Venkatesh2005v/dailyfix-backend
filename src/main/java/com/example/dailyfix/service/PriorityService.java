package com.example.dailyfix.service;

import com.example.dailyfix.enums.*;
import com.example.dailyfix.model.Message;
import com.example.dailyfix.model.MessageInteraction;
import com.example.dailyfix.model.SenderProfile;
import com.example.dailyfix.repository.MessageInteractionRepository;
import com.example.dailyfix.repository.SenderProfileRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PriorityService {

    private final SenderProfileRepository senderProfileRepository;
    private final MessageInteractionRepository messageInteractionRepository;

    public PriorityService(SenderProfileRepository senderProfileRepository,
                           MessageInteractionRepository messageInteractionRepository) {
        this.senderProfileRepository = senderProfileRepository;
        this.messageInteractionRepository = messageInteractionRepository;
    }

    public Priority calculatePriority(Message message) {
        int totalScore = 0;

        totalScore += calculateTrustScore(message);
        totalScore += calculateIntentScore(message);
        totalScore += calculateSourceScore(message);
        totalScore += calculateKeywordScore(message);
        totalScore += calculateUserBehaviorScore(message);

        return mapScoreToPriority(totalScore);
    }

    private int calculateUserBehaviorScore(Message message) {
        String domain = extractDomain(message.getSenderEmail());
        List<MessageInteraction> interactions =
                messageInteractionRepository.findByMessage_SenderDomain(domain);

        if (interactions.isEmpty()) return 0;

        long openedCount = interactions.stream()
                .filter(i -> i.getAction() == InteractionType.OPENED).count();
        long ignoredCount = interactions.stream()
                .filter(i -> i.getAction() == InteractionType.IGNORED).count();

        if (ignoredCount > openedCount) return -40;
        if (openedCount >= ignoredCount) return 10;
        return -10;
    }

    private int calculateTrustScore(Message message) {
        if (message.getSenderEmail() == null) return -10;
        String domain = extractDomain(message.getSenderEmail());
        Optional<SenderProfile> senderProfile = senderProfileRepository.findBySenderDomain(domain);

        if (senderProfile.isEmpty()) return -10;

        TrustLevel trust = senderProfile.get().getTrustLevel();
        return switch (trust) {
            case HIGH -> 40;
            case MEDIUM -> 20;
            case LOW -> -30;
        };
    }

    private int calculateIntentScore(Message message) {
        String subject = message.getSubject() == null ? "" : message.getSubject();
        String content = message.getContent() == null ? "" : message.getContent();
        String text = (subject + " " + content).toLowerCase();

        if (text.contains("failed") || text.contains("action required") || text.contains("urgent")) {
            message.setIntent(MessageIntent.ACTION_REQUIRED);
            return 40;
        }
        if (text.contains("invoice") || text.contains("update") || text.contains("meeting")) {
            message.setIntent(MessageIntent.INFORMATIONAL);
            return 10;
        }
        message.setIntent(MessageIntent.PROMOTIONAL);
        return -50;
    }

    private int calculateSourceScore(Message message) {
        SourceType source = message.getSourceType();
        if (source == null) return 0;
        return switch (source) {
            case SYSTEM -> 30;
            case INTERNAL -> 20;
            case EMAIL -> 0;
        };
    }

    private int calculateKeywordScore(Message message) {
        if (message.getContent() == null) return 0;
        String text = message.getContent().toLowerCase();
        int score = 0;

        // AUTOMATIC DETECTION KEYWORDS
        if (text.contains("error") || text.contains("failed") || text.contains("critical")) score += 25;
        if (text.contains("deadline") || text.contains("asap") || text.contains("immediately")) score += 20;

        return score;
    }

    private Priority mapScoreToPriority(int score) {
        if (score >= 60) return Priority.HIGH;
        if (score >= 30) return Priority.MEDIUM;
        if (score >= 10) return Priority.LOW;
        return Priority.SILENT;
    }

    private String extractDomain(String email) {
        if (!email.contains("@")) return "";
        return email.substring(email.indexOf("@") + 1).toLowerCase();
    }
}