package com.example.dailyfix.model;

import com.example.dailyfix.enums.ActionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Task task;

    @ManyToOne
    private User performedBy;

    @Enumerated(EnumType.STRING)
    private ActionType action;

    private LocalDateTime performedAt;
    private String remarks;
}



