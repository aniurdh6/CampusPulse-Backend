package com.campuspulse.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuspulse.dto.Dtos.UserDto;
import com.campuspulse.exception.ApiException;
import com.campuspulse.model.Role;
import com.campuspulse.model.User;
import com.campuspulse.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository users;
    private final NotificationService notifications;
    private final ActivityLogService logs;

    public UserService(UserRepository users, NotificationService notifications, ActivityLogService logs) {
        this.users = users;
        this.notifications = notifications;
        this.logs = logs;
    }

    @Transactional(readOnly = true)
    public List<UserDto> list() {
        return users.findAllByOrderByNameAsc().stream().map(UserDto::from).toList();
    }

    @Transactional
    public UserDto changeRole(User admin, Long id, Role role) {
        if (admin.getId().equals(id)) {
            throw ApiException.badRequest("You can't change your own role");
        }
        User target = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        if (target.getRole() == role) {
            return UserDto.from(target);
        }
        Role before = target.getRole();
        target.setRole(role);
        notifications.send(target, "Your role was changed to " + role.name().toLowerCase());
        logs.log(admin.getName(), "ROLE_CHANGED", target.getName() + ": " + before + " to " + role);
        return UserDto.from(target);
    }
}
