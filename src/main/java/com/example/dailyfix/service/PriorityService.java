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
        String domain = extractDomain(message.getSenderEmail());

        Optional<SenderProfile> senderProfile =
                senderProfileRepository.findBySenderDomain(domain);

        if(senderProfile.isEmpty()){
            return -10;
        }

        TrustLevel trust = senderProfile.get().getTrustLevel();

        if(trust == TrustLevel.HIGH) return 40;
        else if(trust == TrustLevel.MEDIUM) return 20;
        else return -30;

    }

    private int calculateIntentScore(Message message) {
        String text = (message.getSubject() + " " + message.getContent()).toLowerCase();

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

        if (source == SourceType.SYSTEM) return 30;
        if (source == SourceType.INTERNAL) return 20;
        return 0;
    }

    private int calculateKeywordScore(Message message) {
        String text = message.getContent().toLowerCase();

        if (text.contains("error") || text.contains("failed")) return 20;
        if (text.contains("deadline")) return 15;

        return 0;
    }

    private Priority mapScoreToPriority(int score) {
        if (score >= 60) return Priority.HIGH;
        if (score >= 30) return Priority.MEDIUM;
        if (score >= 10) return Priority.LOW;
        return Priority.SILENT;
    }

    private String extractDomain(String email) {
        return email.substring(email.indexOf("@") + 1).toLowerCase();
    }
}
