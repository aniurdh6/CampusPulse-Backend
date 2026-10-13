package com.campuspulse.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuspulse.dto.Dtos.AuthResponse;
import com.campuspulse.dto.Dtos.LoginRequest;
import com.campuspulse.dto.Dtos.RegisterRequest;
import com.campuspulse.dto.Dtos.UserDto;
import com.campuspulse.exception.ApiException;
import com.campuspulse.model.Role;
import com.campuspulse.model.User;
import com.campuspulse.repository.UserRepository;
import com.campuspulse.security.PasswordHasher;
import com.campuspulse.security.TokenStore;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final TokenStore tokens;
    private final ActivityLogService logs;

    public AuthService(UserRepository users, PasswordHasher hasher, TokenStore tokens, ActivityLogService logs) {
        this.users = users;
        this.hasher = hasher;
        this.tokens = tokens;
        this.logs = logs;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw ApiException.badRequest("Admin accounts are created by an existing admin");
        }
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(hasher.hash(request.password()));
        user.setRole(request.role());
        user.setDepartment(request.department() == null || request.department().isBlank()
                ? null : request.department().trim());
        users.save(user);
        logs.log(user.getName(), "REGISTER", "New " + user.getRole().name().toLowerCase() + " account " + email);
        return new AuthResponse(tokens.issue(user.getId()), UserDto.from(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim()).orElse(null);
        if (user == null || !hasher.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Incorrect email or password");
        }
        logs.log(user.getName(), "LOGIN", "Signed in");
        return new AuthResponse(tokens.issue(user.getId()), UserDto.from(user));
    }

    public void logout(String token) {
        tokens.revoke(token);
    }
}
