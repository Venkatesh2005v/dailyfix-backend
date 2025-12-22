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
public class User {
    @Id
    private int userId;
    private String name;
    private String email;
    private String password;
    private String role;
    private boolean isActive;
    private Date createdAt;
}
