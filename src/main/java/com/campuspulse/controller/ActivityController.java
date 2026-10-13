package com.campuspulse.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campuspulse.dto.Dtos.LogDto;
import com.campuspulse.model.Role;
import com.campuspulse.security.RequireRole;
import com.campuspulse.service.ActivityLogService;

@RestController
@RequestMapping("/api/logs")
@RequireRole(Role.ADMIN)
public class ActivityController {

    private final ActivityLogService logs;

    public ActivityController(ActivityLogService logs) {
        this.logs = logs;
    }

    @GetMapping
    public List<LogDto> latest() {
        return logs.latest();
    }
}
