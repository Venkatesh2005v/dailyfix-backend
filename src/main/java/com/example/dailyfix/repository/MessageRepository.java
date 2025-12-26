package com.example.dailyfix.repository;

import com.example.dailyfix.enums.Priority;
import com.example.dailyfix.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByPriority(Priority priority);

    List<Message> findByProcessedFalse();
}
