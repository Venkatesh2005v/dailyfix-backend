package com.example.dailyfix.repository;

import com.example.dailyfix.enums.TaskStatus;
import com.example.dailyfix.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(TaskStatus status);
    List<Task> findByAssignedToEmail(String email);
    List<Task> findBySourceMessageIdAndAssignedToEmail(Long messageId, String email);

}

