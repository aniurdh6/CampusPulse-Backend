package com.campuspulse.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campuspulse.dto.Dtos.AnalyticsDto;
import com.campuspulse.model.Role;
import com.campuspulse.security.RequireRole;
import com.campuspulse.service.AnalyticsService;

@RestController
@RequestMapping("/api/analytics")
@RequireRole({Role.ADMIN, Role.FACULTY})
public class AnalyticsController {

    private final AnalyticsService analytics;

    public AnalyticsController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    @GetMapping
    public AnalyticsDto summary() {
        return analytics.summary();
    }
}
