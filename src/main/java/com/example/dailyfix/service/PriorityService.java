package com.example.dailyfix.service;

import com.example.dailyfix.enums.MessageIntent;
import com.example.dailyfix.enums.Priority;
import com.example.dailyfix.enums.SourceType;
import com.example.dailyfix.enums.TrustLevel;
import com.example.dailyfix.model.Message;
import com.example.dailyfix.model.SenderProfile;
import com.example.dailyfix.repository.SenderProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PriorityService {

    private final SenderProfileRepository senderProfileRepository;

    public PriorityService(SenderProfileRepository senderProfileRepository) {
        this.senderProfileRepository = senderProfileRepository;
    }


    public Priority calculatePriority(Message message) {

        int totalScore = 0;

        totalScore += calculateTrustScore(message);
        totalScore += calculateIntentScore(message);
        totalScore += calculateSourceScore(message);
        totalScore += calculateKeywordScore(message);

        return mapScoreToPriority(totalScore);
    }

    private int calculateTrustScore(Message message) {

        if (message.getSenderEmail() == null) {
            return -10;
        }

        String domain = extractDomain(message.getSenderEmail());

        Optional<SenderProfile> senderProfile =
                senderProfileRepository.findBySenderDomain(domain);

        if (senderProfile.isEmpty()) {
            return -10;
        }

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

        if (text.contains("failed") || text.contains("action required")) {
            message.setIntent(MessageIntent.ACTION_REQUIRED);
            return 40;
        }

        if (text.contains("invoice") || text.contains("update")) {
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

        if (message.getContent() == null) {
            return 0;
        }

        String text = message.getContent().toLowerCase();
        int score = 0;

        if (text.contains("error") || text.contains("failed")) {
            score += 20;
        }

        if (text.contains("deadline")) {
            score += 15;
        }

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
