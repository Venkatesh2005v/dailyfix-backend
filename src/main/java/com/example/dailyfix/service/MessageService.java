package com.example.dailyfix.service;

import com.example.dailyfix.dto.request.MessageRequest;
import com.example.dailyfix.enums.Priority;
import com.example.dailyfix.enums.SourceType;
import com.example.dailyfix.model.Message;
import com.example.dailyfix.model.User;
import com.example.dailyfix.repository.MessageRepository;
import com.example.dailyfix.repository.UserRepository;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.ModifyMessageRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
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

    public MessageService(MessageRepository messageRepository,
                          PriorityService priorityService,
                          TaskService taskService,
                          UserRepository userRepository,
                          OAuth2AuthorizedClientService authorizedClientService) {
        this.messageRepository = messageRepository;
        this.priorityService = priorityService;
        this.taskService = taskService;
        this.userRepository = userRepository;
        this.authorizedClientService = authorizedClientService;
    }

    /**
     * DIRECT FETCH: Fetches unread emails for the logged-in user using their OAuth2 token.
     */
    public void fetchAndProcessGmail(Authentication authentication) {
        try {
            // 1. Get Access Token from the current OAuth2 session
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    "google", authentication.getName());
            String accessToken = client.getAccessToken().getTokenValue();

            // 2. Initialize Gmail Client
            Gmail gmail = new Gmail.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    null)
                    .setHttpRequestInitializer(request -> request.getHeaders().setAuthorization("Bearer " + accessToken))
                    .setApplicationName("DailyFix")
                    .build();

            // 3. Fetch unread messages
            ListMessagesResponse response = gmail.users().messages().list("me")
                    .setQ("is:unread")
                    .execute();

            if (response.getMessages() != null) {
                for (com.google.api.services.gmail.model.Message msgSummary : response.getMessages()) {
                    com.google.api.services.gmail.model.Message fullEmail =
                            gmail.users().messages().get("me", msgSummary.getId()).execute();

                    // 4. Convert and Save to local DB
                    Message message = mapGmailToEntity(fullEmail, authentication.getName());
                    messageRepository.save(message);

                    // 5. Automate Processing
                    processMessage(message);

                    // 6. Mark as read in Gmail
                    markMessageAsRead(gmail, msgSummary.getId());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch Gmail: " + e.getMessage());
        }
    }

    private Message mapGmailToEntity(com.google.api.services.gmail.model.Message gMsg, String userEmail) {
        Message message = new Message();
        List<MessagePartHeader> headers = gMsg.getPayload().getHeaders();

        String subject = headers.stream().filter(h -> h.getName().equalsIgnoreCase("Subject"))
                .map(MessagePartHeader::getValue).findFirst().orElse("No Subject");
        String from = headers.stream().filter(h -> h.getName().equalsIgnoreCase("From"))
                .map(MessagePartHeader::getValue).findFirst().orElse("Unknown");

        message.setSubject(subject);
        message.setSenderEmail(from);
        message.setContent(gMsg.getSnippet());
        message.setSourceType(SourceType.EMAIL);
        message.setReceivedAt(LocalDateTime.now());
        message.setProcessed(false);

        userRepository.findByEmail(userEmail).ifPresent(message::setUser);
        return message;
    }

    private void markMessageAsRead(Gmail gmail, String messageId) throws IOException {
        ModifyMessageRequest mods = new ModifyMessageRequest()
                .setRemoveLabelIds(Collections.singletonList("UNREAD"));
        gmail.users().messages().modify("me", messageId, mods).execute();
    }

    public void receiveMessage(Long userId, MessageRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Message message = new Message();
        message.setUser(user);
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
        } else {
            message.setProcessed(false);
        }
        messageRepository.save(message);
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }

    public Message getMessageById(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
    }

    public void reprocessMessage(Long messageId) {
        Message message = getMessageById(messageId);
        message.setProcessed(false);
        processMessage(message);
    }

    public List<Message> getMessagesByPriority(Priority priority) {
        return messageRepository.findByPriority(priority);
    }

    public List<Message> getUnprocessedMessages() {
        return messageRepository.findByProcessedFalse();
    }

    public @Nullable List<Message> getMessagesByUserEmail(String userEmail) {
        return messageRepository.getMessagesByUserEmail(userEmail);
    }

    public @Nullable List<Message> getMessagesByUserEmailAndPriority(String userEmail, Priority priority) {
        return messageRepository.getMessagesByUserEmailAndPriority(userEmail, priority);
    }
}