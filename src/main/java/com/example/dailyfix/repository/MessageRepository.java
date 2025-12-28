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

    List<Message> findByPriority(Priority priority);

    List<Message> findByProcessedFalse();

    // Fetches only messages belonging to a specific user email
    List<Message> findByUserEmail(String email);

    // Alternatively, fetch by User object
    List<Message> findByUser(User user);

    @Nullable List<Message> findByUserEmailAndPriority(String userEmail, Priority priority);

    @Nullable List<Message> getMessagesByUserEmail(String userEmail);

    @Nullable List<Message> getMessagesByUserEmailAndPriority(String userEmail, Priority priority);
}

