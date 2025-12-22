package com.example.dailyfix.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Task {
    @Id
    private int taskId;
    private String title;
    private String description;
    private String priority;
    private String status;
    private Date dueDate;
    private String assignedTo;
    private int messageId;
    private Date createdAt;
    private Date completedAt;
}
