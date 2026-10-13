package com.campuspulse.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuspulse.dto.Dtos.LogDto;
import com.campuspulse.model.ActivityLog;
import com.campuspulse.repository.ActivityLogRepository;

@Service
public class ActivityLogService {

    private final ActivityLogRepository logs;

    public ActivityLogService(ActivityLogRepository logs) {
        this.logs = logs;
    }

    @Transactional
    public void log(String actor, String action, String detail) {
        ActivityLog entry = new ActivityLog();
        entry.setActor(actor);
        entry.setAction(action);
        entry.setDetail(detail == null ? null : detail.length() > 500 ? detail.substring(0, 500) : detail);
        logs.save(entry);
    }

    @Transactional(readOnly = true)
    public List<LogDto> latest() {
        return logs.findTop100ByOrderByCreatedAtDesc().stream().map(LogDto::from).toList();
    }
}
