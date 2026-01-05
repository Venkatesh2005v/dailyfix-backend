package com.example.dailyfix.repository;

import com.example.dailyfix.model.AlertWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlertWhitelistRepository extends JpaRepository<AlertWhitelist, Long> {
    Optional<AlertWhitelist> findBySenderDomain(String senderDomain);
}
