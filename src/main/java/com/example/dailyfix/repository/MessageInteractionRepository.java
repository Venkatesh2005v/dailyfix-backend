package com.example.dailyfix.repository;

import com.example.dailyfix.model.MessageInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageInteractionRepository extends JpaRepository<MessageInteraction, Long> {
    List<MessageInteraction> findByMessage_SenderDomain(String senderDomain);

}
