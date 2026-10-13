package com.campuspulse.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campuspulse.dto.Dtos.RoleUpdate;
import com.campuspulse.dto.Dtos.UserDto;
import com.campuspulse.model.Role;
import com.campuspulse.model.User;
import com.campuspulse.security.RequireRole;
import com.campuspulse.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@RequireRole(Role.ADMIN)
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public List<UserDto> list() {
        return users.list();
    }

    @PatchMapping("/{id}/role")
    public UserDto changeRole(@RequestAttribute("currentUser") User admin, @PathVariable Long id,
                              @Valid @RequestBody RoleUpdate update) {
        return users.changeRole(admin, id, update.role());
    }
}
