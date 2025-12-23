package com.example.dailyfix.model;

import com.example.dailyfix.enums.InteractionType;
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
public class MessageInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Message message;

    @Enumerated(EnumType.STRING)
    private InteractionType action;

    private LocalDateTime interactedAt;
}

