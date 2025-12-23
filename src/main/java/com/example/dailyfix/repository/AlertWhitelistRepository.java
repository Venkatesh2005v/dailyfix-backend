package com.example.dailyfix.repository;

import com.example.dailyfix.model.AlertWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertWhitelistRepository extends JpaRepository<AlertWhitelist, Long> {
}
