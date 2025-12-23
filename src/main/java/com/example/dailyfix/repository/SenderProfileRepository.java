package com.example.dailyfix.repository;

import com.example.dailyfix.model.SenderProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SenderProfileRepository extends JpaRepository<SenderProfile, Long> {
}
