package com.campuspulse.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campuspulse.dto.Dtos.NotificationDto;
import com.campuspulse.model.User;
import com.campuspulse.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public List<NotificationDto> list(@RequestAttribute("currentUser") User user) {
        return notifications.list(user);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@RequestAttribute("currentUser") User user) {
        return Map.of("count", notifications.unreadCount(user));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@RequestAttribute("currentUser") User user, @PathVariable Long id) {
        notifications.markRead(user, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@RequestAttribute("currentUser") User user) {
        notifications.markAllRead(user);
        return ResponseEntity.noContent().build();
    }
}
