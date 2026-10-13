package com.campuspulse.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import com.campuspulse.model.ActivityLog;
import com.campuspulse.model.Booking;
import com.campuspulse.model.BookingStatus;
import com.campuspulse.model.CampusResource;
import com.campuspulse.model.MaintenanceRequest;
import com.campuspulse.model.MaintenanceStatus;
import com.campuspulse.model.Notification;
import com.campuspulse.model.Priority;
import com.campuspulse.model.ResourceType;
import com.campuspulse.model.Role;
import com.campuspulse.model.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request and response shapes for the REST API. Entities never leave the service layer. */
public final class Dtos {

    private Dtos() {}

    // ---- auth and users ----

    public record RegisterRequest(
            @NotBlank(message = "Name is required") @Size(max = 100, message = "Name is too long") String name,
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") String email,
            @NotBlank(message = "Password is required")
            @Size(min = 6, max = 100, message = "Password must be 6 to 100 characters") String password,
            @NotNull(message = "Choose student or faculty") Role role,
            @Size(max = 100, message = "Department is too long") String department) {}

    public record LoginRequest(
            @NotBlank(message = "Email is required") String email,
            @NotBlank(message = "Password is required") String password) {}

    public record UserDto(Long id, String name, String email, Role role, String department) {
        public static UserDto from(User u) {
            return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getDepartment());
        }
    }

    public record AuthResponse(String token, UserDto user) {}

    public record RoleUpdate(@NotNull(message = "Role is required") Role role) {}

    // ---- resources ----

    public record ResourceRequest(
            @NotBlank(message = "Name is required") @Size(max = 120, message = "Name is too long") String name,
            @NotNull(message = "Type is required") ResourceType type,
            @NotBlank(message = "Location is required") @Size(max = 150, message = "Location is too long") String location,
            @Min(value = 1, message = "Capacity must be at least 1") int capacity,
            @Size(max = 300, message = "Features list is too long") String features,
            @Size(max = 500, message = "Description is too long") String description,
            Boolean active) {}

    public record ResourceDto(Long id, String name, ResourceType type, String location, int capacity,
                              List<String> features, String description, boolean active) {
        public static ResourceDto from(CampusResource r) {
            return new ResourceDto(r.getId(), r.getName(), r.getType(), r.getLocation(), r.getCapacity(),
                    r.featureList(), r.getDescription(), r.isActive());
        }
    }

    public record SlotDto(LocalTime startTime, LocalTime endTime, String bookedBy, BookingStatus status) {}

    // ---- bookings ----

    public record BookingRequest(
            @NotNull(message = "Choose a resource") Long resourceId,
            @NotNull(message = "Choose a date") LocalDate date,
            @NotNull(message = "Choose a start time") LocalTime startTime,
            @NotNull(message = "Choose an end time") LocalTime endTime,
            @Size(max = 255, message = "Purpose is too long") String purpose) {}

    public record BookingDto(Long id, Long resourceId, String resourceName, ResourceType resourceType,
                             String resourceLocation, Long userId, String userName, LocalDate date,
                             LocalTime startTime, LocalTime endTime, String purpose, BookingStatus status,
                             String decisionNote, LocalDateTime createdAt) {
        public static BookingDto from(Booking b) {
            CampusResource r = b.getResource();
            User u = b.getUser();
            return new BookingDto(b.getId(), r.getId(), r.getName(), r.getType(), r.getLocation(), u.getId(),
                    u.getName(), b.getBookingDate(), b.getStartTime(), b.getEndTime(), b.getPurpose(),
                    b.getStatus(), b.getDecisionNote(), b.getCreatedAt());
        }
    }

    public record DecisionRequest(@Size(max = 255, message = "Reason is too long") String note) {}

    // ---- maintenance ----

    public record MaintenanceCreate(
            Long resourceId,
            @NotBlank(message = "Say where the problem is") @Size(max = 150, message = "Location is too long") String location,
            @NotBlank(message = "Give the issue a short title") @Size(max = 150, message = "Title is too long") String title,
            @NotBlank(message = "Describe what is wrong") @Size(max = 1000, message = "Description is too long") String description,
            @NotNull(message = "Choose a priority") Priority priority) {}

    public record MaintenanceStatusUpdate(
            @NotNull(message = "Choose a status") MaintenanceStatus status,
            @Size(max = 100, message = "Assignee name is too long") String assignedTo,
            @Size(max = 500, message = "Note is too long") String note) {}

    public record MaintenanceDto(Long id, Long reporterId, String reporterName, Long resourceId, String resourceName,
                                 String location, String title, String description, Priority priority,
                                 MaintenanceStatus status, String assignedTo, String adminNote,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        public static MaintenanceDto from(MaintenanceRequest m) {
            CampusResource r = m.getResource();
            return new MaintenanceDto(m.getId(), m.getReporter().getId(), m.getReporter().getName(),
                    r == null ? null : r.getId(), r == null ? null : r.getName(), m.getLocation(), m.getTitle(),
                    m.getDescription(), m.getPriority(), m.getStatus(), m.getAssignedTo(), m.getAdminNote(),
                    m.getCreatedAt(), m.getUpdatedAt());
        }
    }

    // ---- recommendations ----

    public record RecommendationRequest(
            ResourceType type,
            @Min(value = 1, message = "Capacity must be at least 1") Integer minCapacity,
            List<String> features,
            @NotNull(message = "Choose a date") LocalDate date,
            @NotNull(message = "Choose a start time") LocalTime startTime,
            @NotNull(message = "Choose an end time") LocalTime endTime) {}

    public record RecommendationDto(ResourceDto resource, int score, List<String> reasons) {}

    // ---- notifications and logs ----

    public record NotificationDto(Long id, String message, boolean seen, LocalDateTime createdAt) {
        public static NotificationDto from(Notification n) {
            return new NotificationDto(n.getId(), n.getMessage(), n.isSeen(), n.getCreatedAt());
        }
    }

    public record LogDto(Long id, String actor, String action, String detail, LocalDateTime createdAt) {
        public static LogDto from(ActivityLog l) {
            return new LogDto(l.getId(), l.getActor(), l.getAction(), l.getDetail(), l.getCreatedAt());
        }
    }

    // ---- analytics ----

    public record NameCount(String name, long count) {}

    public record DayCount(LocalDate date, long count) {}

    public record AnalyticsDto(long totalResources, long activeResources, long totalBookings, long bookingsToday,
                               long openIssues, double utilizationPercent, double avgResolutionHours,
                               Map<String, Long> bookingsByType, List<NameCount> topResources,
                               Map<String, Long> issuesByStatus, Map<String, Long> openIssuesByPriority,
                               List<DayCount> bookingsLast7Days) {}
}
