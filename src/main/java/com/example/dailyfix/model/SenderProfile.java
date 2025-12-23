package com.example.dailyfix.model;

import com.example.dailyfix.enums.TrustLevel;
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
public class SenderProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String senderDomain;

    @Enumerated(EnumType.STRING)
    private TrustLevel trustLevel;

    private boolean promotional;

    private LocalDateTime createdAt;
}
