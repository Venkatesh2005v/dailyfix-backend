package com.example.dailyfix.repository;

import com.example.dailyfix.enums.Priority;
import com.example.dailyfix.model.Message;
import com.example.dailyfix.model.User;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    boolean existsByGmailId(String gmailId);
    List<Message> findByUserEmail(String email);
    List<Message> findByUserEmailAndPriority(String email, Priority priority);
}

