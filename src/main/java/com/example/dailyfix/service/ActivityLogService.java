package com.example.dailyfix.service;

import com.example.dailyfix.enums.ActionType;
import com.example.dailyfix.model.ActivityLog;
import com.example.dailyfix.model.Task;
import com.example.dailyfix.model.User;
import com.example.dailyfix.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    public void logActivity(Task task, User user, ActionType action, String remarks) {

        ActivityLog log = new ActivityLog();
        log.setTask(task);
        log.setPerformedBy(user);
        log.setAction(action);
        log.setPerformedAt(LocalDateTime.now());
        log.setRemarks(remarks);

        activityLogRepository.save(log);
    }
}
