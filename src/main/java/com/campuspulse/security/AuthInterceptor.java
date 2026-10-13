package com.campuspulse.security;

import java.util.Arrays;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.campuspulse.exception.ApiException;
import com.campuspulse.model.User;
import com.campuspulse.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Checks the bearer token on every /api request (except login and register),
 * loads the user, and enforces @RequireRole. Controllers read the user with
 * @RequestAttribute("currentUser").
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenStore tokens;
    private final UserRepository users;

    public AuthInterceptor(TokenStore tokens, UserRepository users) {
        this.tokens = tokens;
        this.users = users;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw ApiException.unauthorized("Sign in to continue");
        }
        String token = header.substring(7).trim();

        Long userId = tokens.resolve(token)
                .orElseThrow(() -> ApiException.unauthorized("Your session has expired. Sign in again"));
        User user = users.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("This account no longer exists"));

        request.setAttribute("currentUser", user);
        request.setAttribute("token", token);

        if (handler instanceof HandlerMethod method) {
            RequireRole required = method.getMethodAnnotation(RequireRole.class);
            if (required == null) {
                required = method.getBeanType().getAnnotation(RequireRole.class);
            }
            if (required != null && Arrays.stream(required.value()).noneMatch(r -> r == user.getRole())) {
                throw ApiException.forbidden("Your role can't do this");
            }
        }
        return true;
    }
}
