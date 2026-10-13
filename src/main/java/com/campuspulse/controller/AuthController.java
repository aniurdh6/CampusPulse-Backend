package com.campuspulse.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.campuspulse.dto.Dtos.AuthResponse;
import com.campuspulse.dto.Dtos.LoginRequest;
import com.campuspulse.dto.Dtos.RegisterRequest;
import com.campuspulse.dto.Dtos.UserDto;
import com.campuspulse.model.User;
import com.campuspulse.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }

    @GetMapping("/me")
    public UserDto me(@RequestAttribute("currentUser") User currentUser) {
        return UserDto.from(currentUser);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestAttribute("token") String token) {
        auth.logout(token);
        return ResponseEntity.noContent().build();
    }
}
