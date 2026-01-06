package com.example.dailyfix.model;

import com.example.dailyfix.enums.InteractionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id") // Best practice to explicitly name join columns
    private User user;

    @ManyToOne
    @JoinColumn(name = "message_id")
    private Message message;

    @Enumerated(EnumType.STRING)
    private InteractionType action;

    private LocalDateTime interactedAt;

    // --- NEW FIELD ---
    @Column(columnDefinition = "TEXT")
    private String feedbackNotes; // Stores "Spam", "Not Urgent", etc.
}