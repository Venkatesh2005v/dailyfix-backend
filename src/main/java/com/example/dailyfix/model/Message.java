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
public class Message {
    @Id
    private int messageId;
    private String senderEmail;
    private String senderDomain;
    private String sourceType;
    private String subject;
    private String content;
    private Date receivedAt;
    private String intent;
    private String priority;
    private boolean processed;
    private String createdBy;

}
