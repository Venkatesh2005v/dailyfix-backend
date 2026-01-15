package com.example.dailyfix.service;

import com.example.dailyfix.enums.*;
import com.example.dailyfix.model.*;
import com.example.dailyfix.repository.*;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.ModifyMessageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final PriorityService priorityService;
    private final TaskService taskService;
    private final UserRepository userRepository;
    private final AlertWhitelistRepository alertWhitelistRepository;
    private final SenderProfileRepository senderProfileRepository;

    @Autowired
    private OAuth2AuthorizedClientManager authorizedClientManager;

    public MessageService(MessageRepository messageRepository,
                          PriorityService priorityService,
                          TaskService taskService,
                          UserRepository userRepository,
                          AlertWhitelistRepository alertWhitelistRepository,
                          SenderProfileRepository senderProfileRepository) {
        this.messageRepository = messageRepository;
        this.priorityService = priorityService;
        this.taskService = taskService;
        this.userRepository = userRepository;
        this.alertWhitelistRepository = alertWhitelistRepository;
        this.senderProfileRepository = senderProfileRepository;
    }

    /**
     * Entry point for manual sync from the Frontend.
     */
    @Async
    public void fetchAndProcessGmail(Authentication authentication) {
        String email = (authentication instanceof OAuth2AuthenticationToken oauthToken)
                ? oauthToken.getPrincipal().getAttribute("email")
                : authentication.getName();

        System.out.println("--- Starting Manual Gmail Sync for: " + email + " ---");
        processWithToken(email);
    }

    /**
     * Core logic used by both manual sync and background scheduler.
     * Uses OAuth2AuthorizedClientManager to handle automatic token refresh.
     */
    public void processWithToken(String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found: " + email));

            // 1. Get/Refresh the Access Token
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId("google")
                    .principal(email)
                    .build();

            // This requires AuthorizedClientServiceOAuth2AuthorizedClientManager in your Config
            OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);

            if (authorizedClient == null) {
                System.err.println("CRITICAL: No authorized client found for " + email + ". User must re-login.");
                return;
            }

            String freshAccessToken = authorizedClient.getAccessToken().getTokenValue();

            // 2. Build Gmail API Client
            Gmail gmail = new Gmail.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    null)
                    .setHttpRequestInitializer(request ->
                            request.getHeaders().setAuthorization("Bearer " + freshAccessToken))
                    .setApplicationName("DailyFix")
                    .build();

            // 3. Fetch messages from last 3 days
            String query = "label:INBOX newer_than:3d";
            ListMessagesResponse response = gmail.users().messages().list("me").setQ(query).execute();

            if (response.getMessages() != null) {
                for (com.google.api.services.gmail.model.Message msgSummary : response.getMessages()) {

                    if (messageRepository.existsByGmailId(msgSummary.getId())) continue;

                    com.google.api.services.gmail.model.Message fullEmail =
                            gmail.users().messages().get("me", msgSummary.getId()).execute();

                    Message message = mapGmailToEntity(fullEmail, user);
                    message.setGmailId(msgSummary.getId());
                    message = messageRepository.save(message);

                    // --- FIX FOR 429 ERRORS: Isolated Priority Processing ---
                    try {
                        processMessage(message);

                        // Pause for 2 seconds between emails to avoid Gemini Free Tier rate limits
                        Thread.sleep(2000);
                    } catch (Exception aiEx) {
                        System.err.println("Gemini analysis failed for message: " + message.getGmailId() + " - " + aiEx.getMessage());
                        // We don't throw the error here so that the loop continues to the next email
                    }

                    markMessageAsRead(gmail, msgSummary.getId());
                }
            }
            System.out.println("Sync successfully completed for: " + email);

        } catch (Exception e) {
            // If we hit a 401 here, it's likely the Refresh Token itself is expired/revoked
            System.err.println("Outer Sync Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Analyzes message priority and generates tasks for High/Medium priority items.
     */
    @Transactional
    public void processMessage(Message message) {
        String domain = message.getSenderDomain();

        // Check whitelist
        boolean isWhitelisted = alertWhitelistRepository.findBySenderDomain(domain)
                .map(AlertWhitelist::isAlertEnabled)
                .orElse(false);

        if (!isWhitelisted) {
            message.setPriority(Priority.SILENT);
            message.setProcessed(true);
            messageRepository.save(message);
            return;
        }

        // Determine Trust & Calculate Priority via Gemini
        TrustLevel trust = senderProfileRepository.findBySenderDomain(domain)
                .map(SenderProfile::getTrustLevel)
                .orElse(TrustLevel.LOW);

        Priority priority = priorityService.calculatePriority(message, trust);
        message.setPriority(priority);

        // Auto-create task if important
        if (priority == Priority.HIGH || priority == Priority.MEDIUM) {
            taskService.createTaskFromMessage(message);
        }

        message.setProcessed(true);
        messageRepository.save(message);
    }

    // --- DATA RETRIEVAL METHODS ---

    public List<Message> getMessagesByUserEmail(String userEmail) {
        return messageRepository.findByUserEmail(userEmail);
    }

    public List<Message> getMessagesByUserEmailAndPriority(String userEmail, Priority priority) {
        return messageRepository.findByUserEmailAndPriority(userEmail, priority);
    }

    public void reprocessMessage(Long id) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        message.setProcessed(false);
        processMessage(message);
    }

    // --- PRIVATE HELPERS ---

    private Message mapGmailToEntity(com.google.api.services.gmail.model.Message gMsg, User user) {
        Message message = new Message();
        List<MessagePartHeader> headers = gMsg.getPayload().getHeaders();

        // Extract "Subject" and "From" from headers
        String subject = headers.stream()
                .filter(h -> h.getName().equalsIgnoreCase("Subject"))
                .map(MessagePartHeader::getValue).findFirst().orElse("No Subject");

        String from = headers.stream()
                .filter(h -> h.getName().equalsIgnoreCase("From"))
                .map(MessagePartHeader::getValue).findFirst().orElse("Unknown");

        message.setSubject(subject);
        message.setSenderEmail(from);

        // Extract Domain for Whitelist checking
        if (from != null && from.contains("@")) {
            try {
                String domain = from.substring(from.indexOf("@") + 1).split(">")[0].trim();
                message.setSenderDomain(domain);
            } catch (Exception e) {
                message.setSenderDomain("unknown.com");
            }
        }

        message.setContent(gMsg.getSnippet());
        message.setSourceType(SourceType.EMAIL);
        message.setReceivedAt(LocalDateTime.now());
        message.setProcessed(false);
        message.setUser(user);

        return message;
    }

    private void markMessageAsRead(Gmail gmail, String messageId) throws IOException {
        ModifyMessageRequest mods = new ModifyMessageRequest()
                .setRemoveLabelIds(Collections.singletonList("UNREAD"));
        gmail.users().messages().modify("me", messageId, mods).execute();
    }
}