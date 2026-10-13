package com.campuspulse.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.campuspulse.dto.Dtos.ResourceDto;
import com.campuspulse.dto.Dtos.ResourceRequest;
import com.campuspulse.dto.Dtos.SlotDto;
import com.campuspulse.model.ResourceType;
import com.campuspulse.model.Role;
import com.campuspulse.model.User;
import com.campuspulse.security.RequireRole;
import com.campuspulse.service.ResourceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resources;

    public ResourceController(ResourceService resources) {
        this.resources = resources;
    }

    @GetMapping
    public List<ResourceDto> list(@RequestParam(required = false) ResourceType type) {
        return resources.list(type);
    }

    /** Booked slots for every resource on one day, keyed by resource id. */
    @GetMapping("/availability")
    public Map<Long, List<SlotDto>> availability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return resources.availability(date);
    }

    @GetMapping("/{id}")
    public ResourceDto get(@PathVariable Long id) {
        return resources.get(id);
    }

    @PostMapping
    @RequireRole(Role.ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceDto create(@RequestAttribute("currentUser") User admin,
                              @Valid @RequestBody ResourceRequest request) {
        return resources.create(admin, request);
    }

    @PutMapping("/{id}")
    @RequireRole(Role.ADMIN)
    public ResourceDto update(@RequestAttribute("currentUser") User admin, @PathVariable Long id,
                              @Valid @RequestBody ResourceRequest request) {
        return resources.update(admin, id, request);
    }

    @DeleteMapping("/{id}")
    @RequireRole(Role.ADMIN)
    public ResponseEntity<Void> delete(@RequestAttribute("currentUser") User admin, @PathVariable Long id) {
        resources.delete(admin, id);
        return ResponseEntity.noContent().build();
    }
}
