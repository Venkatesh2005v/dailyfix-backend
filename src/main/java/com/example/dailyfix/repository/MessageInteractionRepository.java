package com.example.dailyfix.repository;

import com.example.dailyfix.model.MessageInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageInteractionRepository extends JpaRepository<MessageInteraction, Long> {
}
