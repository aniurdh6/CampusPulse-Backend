package com.campuspulse.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.campuspulse.dto.Dtos.MaintenanceCreate;
import com.campuspulse.dto.Dtos.MaintenanceDto;
import com.campuspulse.dto.Dtos.MaintenanceStatusUpdate;
import com.campuspulse.model.MaintenanceStatus;
import com.campuspulse.model.Role;
import com.campuspulse.model.User;
import com.campuspulse.security.RequireRole;
import com.campuspulse.service.MaintenanceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    private final MaintenanceService maintenance;

    public MaintenanceController(MaintenanceService maintenance) {
        this.maintenance = maintenance;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceDto create(@RequestAttribute("currentUser") User user,
                                 @Valid @RequestBody MaintenanceCreate request) {
        return maintenance.create(user, request);
    }

    @GetMapping("/mine")
    public List<MaintenanceDto> mine(@RequestAttribute("currentUser") User user) {
        return maintenance.mine(user);
    }

    @GetMapping
    @RequireRole(Role.ADMIN)
    public List<MaintenanceDto> all(@RequestParam(required = false) MaintenanceStatus status) {
        return maintenance.all(status);
    }

    @PatchMapping("/{id}/status")
    @RequireRole(Role.ADMIN)
    public MaintenanceDto updateStatus(@RequestAttribute("currentUser") User admin, @PathVariable Long id,
                                       @Valid @RequestBody MaintenanceStatusUpdate update) {
        return maintenance.updateStatus(admin, id, update);
    }
}
