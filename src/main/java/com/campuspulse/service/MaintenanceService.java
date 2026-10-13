package com.campuspulse.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuspulse.dto.Dtos.MaintenanceCreate;
import com.campuspulse.dto.Dtos.MaintenanceDto;
import com.campuspulse.dto.Dtos.MaintenanceStatusUpdate;
import com.campuspulse.exception.ApiException;
import com.campuspulse.model.CampusResource;
import com.campuspulse.model.MaintenanceRequest;
import com.campuspulse.model.MaintenanceStatus;
import com.campuspulse.model.User;
import com.campuspulse.repository.MaintenanceRepository;
import com.campuspulse.repository.ResourceRepository;

@Service
public class MaintenanceService {

    private final MaintenanceRepository requests;
    private final ResourceRepository resources;
    private final NotificationService notifications;
    private final ActivityLogService logs;
    private final Clock clock;

    public MaintenanceService(MaintenanceRepository requests, ResourceRepository resources,
                              NotificationService notifications, ActivityLogService logs, Clock clock) {
        this.requests = requests;
        this.resources = resources;
        this.notifications = notifications;
        this.logs = logs;
        this.clock = clock;
    }

    @Transactional
    public MaintenanceDto create(User reporter, MaintenanceCreate input) {
        MaintenanceRequest request = new MaintenanceRequest();
        request.setReporter(reporter);
        if (input.resourceId() != null) {
            CampusResource resource = resources.findById(input.resourceId())
                    .orElseThrow(() -> ApiException.badRequest("That resource doesn't exist"));
            request.setResource(resource);
        }
        request.setLocation(input.location().trim());
        request.setTitle(input.title().trim());
        request.setDescription(input.description().trim());
        request.setPriority(input.priority());
        request.setStatus(MaintenanceStatus.OPEN);
        requests.save(request);

        notifications.sendToAdmins(input.priority() + " priority issue from " + reporter.getName()
                + ": " + request.getTitle() + " (" + request.getLocation() + ")");
        logs.log(reporter.getName(), "ISSUE_REPORTED", request.getTitle() + " at " + request.getLocation());
        return MaintenanceDto.from(request);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceDto> mine(User user) {
        return requests.findByReporterIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(MaintenanceDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MaintenanceDto> all(MaintenanceStatus status) {
        List<MaintenanceRequest> found = status == null
                ? requests.findAllByOrderByCreatedAtDesc()
                : requests.findByStatusOrderByCreatedAtDesc(status);
        return found.stream().map(MaintenanceDto::from).toList();
    }

    /** Moves an issue forward: Open, Assigned, In progress, Resolved. It can't go backwards. */
    @Transactional
    public MaintenanceDto updateStatus(User admin, Long id, MaintenanceStatusUpdate update) {
        MaintenanceRequest request = requests.findById(id)
                .orElseThrow(() -> ApiException.notFound("Issue not found"));
        MaintenanceStatus before = request.getStatus();

        if (before == MaintenanceStatus.RESOLVED) {
            throw ApiException.conflict("This issue is already resolved");
        }
        if (update.status().ordinal() < before.ordinal()) {
            throw ApiException.badRequest("An issue can't move back to an earlier status");
        }

        String assignee = clean(update.assignedTo());
        if (assignee != null) {
            request.setAssignedTo(assignee);
        }
        if (update.status() != MaintenanceStatus.OPEN && request.getAssignedTo() == null) {
            throw ApiException.badRequest("Say who it is assigned to before moving it forward");
        }
        String note = clean(update.note());
        if (note != null) {
            request.setAdminNote(note);
        }
        request.setStatus(update.status());
        request.setUpdatedAt(LocalDateTime.now(clock));

        if (update.status() != before) {
            notifications.send(request.getReporter(), "Your issue \"" + request.getTitle() + "\" is now "
                    + pretty(update.status()) + (request.getAssignedTo() != null ? " (" + request.getAssignedTo() + ")" : ""));
            logs.log(admin.getName(), "ISSUE_STATUS", request.getTitle() + ": " + pretty(before) + " to " + pretty(update.status()));
        }
        return MaintenanceDto.from(request);
    }

    private static String clean(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String pretty(MaintenanceStatus status) {
        return status.name().toLowerCase().replace('_', ' ');
    }
}
