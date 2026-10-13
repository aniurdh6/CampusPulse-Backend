package com.campuspulse.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuspulse.dto.Dtos.NotificationDto;
import com.campuspulse.exception.ApiException;
import com.campuspulse.model.Notification;
import com.campuspulse.model.Role;
import com.campuspulse.model.User;
import com.campuspulse.repository.NotificationRepository;
import com.campuspulse.repository.UserRepository;

@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final UserRepository users;

    public NotificationService(NotificationRepository notifications, UserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    @Transactional
    public void send(User user, String message) {
        Notification n = new Notification();
        n.setUser(user);
        n.setMessage(message.length() > 500 ? message.substring(0, 500) : message);
        notifications.save(n);
    }

    @Transactional
    public void sendToAdmins(String message) {
        users.findByRole(Role.ADMIN).forEach(admin -> send(admin, message));
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> list(User user) {
        return notifications.findTop100ByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(NotificationDto::from).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(User user) {
        return notifications.countByUserIdAndSeenFalse(user.getId());
    }

    @Transactional
    public void markRead(User user, Long id) {
        Notification n = notifications.findById(id)
                .orElseThrow(() -> ApiException.notFound("Notification not found"));
        if (!n.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("That notification belongs to someone else");
        }
        n.setSeen(true);
    }

    @Transactional
    public void markAllRead(User user) {
        notifications.findByUserIdAndSeenFalse(user.getId()).forEach(n -> n.setSeen(true));
    }
}
