package com.example.dailyfix.dto.request;

import com.example.dailyfix.enums.SourceType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageRequest {
    private String senderEmail;

    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    private String subject;

    @Column(length = 2000)
    private String content;

}
