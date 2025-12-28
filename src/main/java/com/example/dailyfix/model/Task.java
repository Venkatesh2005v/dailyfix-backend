package com.example.dailyfix.model;

import com.example.dailyfix.enums.Priority;
import com.example.dailyfix.enums.TaskStatus;
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
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;


    @ManyToOne
    private User assignedTo;

    @OneToOne
    private Message sourceMessage;

    private LocalDateTime createdAt;
    private LocalDateTime dueDate;
}

