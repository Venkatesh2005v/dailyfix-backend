package com.example.dailyfix.model;

import com.example.dailyfix.enums.MessageIntent;
import com.example.dailyfix.enums.Priority;
import com.example.dailyfix.enums.SourceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    private String senderEmail;
    private String senderDomain;

    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime receivedAt;

    @Enumerated(EnumType.STRING)
    private MessageIntent intent;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    private boolean processed;

    @Column(unique = true)
    private String gmailId;


}
