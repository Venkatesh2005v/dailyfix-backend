package com.example.dailyfix.repository;

import com.example.dailyfix.enums.Role;
import com.example.dailyfix.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findFirstByRole(Role role);

    User findByName(String name);

    Optional<User> findByEmail(String userEmail);
}
