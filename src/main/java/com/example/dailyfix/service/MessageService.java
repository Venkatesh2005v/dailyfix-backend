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
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
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
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final AlertWhitelistRepository alertWhitelistRepository;
    private final SenderProfileRepository senderProfileRepository;

    public MessageService(MessageRepository messageRepository,
                          PriorityService priorityService,
                          TaskService taskService,
                          UserRepository userRepository,
                          OAuth2AuthorizedClientService authorizedClientService,
                          AlertWhitelistRepository alertWhitelistRepository,
                          SenderProfileRepository senderProfileRepository) {
        this.messageRepository = messageRepository;
        this.priorityService = priorityService;
        this.taskService = taskService;
        this.userRepository = userRepository;
        this.authorizedClientService = authorizedClientService;
        this.alertWhitelistRepository = alertWhitelistRepository;
        this.senderProfileRepository = senderProfileRepository;
    }

    @Async
    public void fetchAndProcessGmail(Authentication authentication) {
        String email = (authentication instanceof OAuth2AuthenticationToken oauthToken)
                ? oauthToken.getPrincipal().getAttribute("email")
                : authentication.getName();

        System.out.println("--- Starting Gmail Sync for: " + email + " ---");
        processWithToken(email);
    }

    public void processWithToken(String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found: " + email));

            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("google", email);
            if (client == null) return;

            String accessToken = client.getAccessToken().getTokenValue();

            Gmail gmail = new Gmail.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(), null)
                    .setHttpRequestInitializer(request -> request.getHeaders().setAuthorization("Bearer " + accessToken))
                    .setApplicationName("DailyFix").build();

            String query = "{category:primary category:social category:promotions category:updates} newer_than:1d";
            ListMessagesResponse response = gmail.users().messages().list("me").setQ(query).execute();

            if (response.getMessages() != null) {
                for (com.google.api.services.gmail.model.Message msgSummary : response.getMessages()) {

                    if (messageRepository.existsByGmailId(msgSummary.getId())) continue;

                    com.google.api.services.gmail.model.Message fullEmail =
                            gmail.users().messages().get("me", msgSummary.getId()).execute();

                    Message message = mapGmailToEntity(fullEmail, user);
                    message.setGmailId(msgSummary.getId());

                    message = messageRepository.save(message);

                    processMessage(message);

                    markMessageAsRead(gmail, msgSummary.getId());
                }
            }
        } catch (Exception e) {
            System.err.println("Sync Error: " + e.getMessage());
        }
    }

    @Transactional
    public void processMessage(Message message) {
        String domain = message.getSenderDomain();

        boolean isWhitelisted = alertWhitelistRepository.findBySenderDomain(domain)
                .map(AlertWhitelist::isAlertEnabled)
                .orElse(false);

        if (!isWhitelisted) {
            message.setPriority(Priority.SILENT);
            message.setProcessed(true);
            messageRepository.save(message);
            return;
        }

        TrustLevel trust = senderProfileRepository.findBySenderDomain(domain)
                .map(SenderProfile::getTrustLevel)
                .orElse(TrustLevel.LOW);

        Priority priority = priorityService.calculatePriority(message, trust);
        message.setPriority(priority);

        if (priority == Priority.HIGH || priority == Priority.MEDIUM) {
            taskService.createTaskFromMessage(message);
        }

        message.setProcessed(true);
        messageRepository.save(message);
    }

    // --- RE-ADDED MISSING METHODS TO RESOLVE COMPILER ERRORS ---

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

        String subject = headers.stream().filter(h -> h.getName().equalsIgnoreCase("Subject"))
                .map(MessagePartHeader::getValue).findFirst().orElse("No Subject");

        String from = headers.stream().filter(h -> h.getName().equalsIgnoreCase("From"))
                .map(MessagePartHeader::getValue).findFirst().orElse("Unknown");

        message.setSubject(subject);
        message.setSenderEmail(from);

        if (from.contains("@")) {
            String domain = from.substring(from.indexOf("@") + 1).replace(">", "").trim();
            message.setSenderDomain(domain);
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