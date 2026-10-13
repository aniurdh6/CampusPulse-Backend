package com.campuspulse.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.campuspulse.dto.Dtos.BookingDto;
import com.campuspulse.dto.Dtos.BookingRequest;
import com.campuspulse.dto.Dtos.DecisionRequest;
import com.campuspulse.model.Role;
import com.campuspulse.model.User;
import com.campuspulse.security.RequireRole;
import com.campuspulse.service.BookingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookings;

    public BookingController(BookingService bookings) {
        this.bookings = bookings;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDto create(@RequestAttribute("currentUser") User user,
                             @Valid @RequestBody BookingRequest request) {
        return bookings.create(user, request);
    }

    @GetMapping("/mine")
    public List<BookingDto> mine(@RequestAttribute("currentUser") User user) {
        return bookings.mine(user);
    }

    @GetMapping
    @RequireRole(Role.ADMIN)
    public List<BookingDto> all(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookings.all(date);
    }

    @GetMapping("/pending")
    @RequireRole(Role.ADMIN)
    public List<BookingDto> pending() {
        return bookings.pending();
    }

    @PostMapping("/{id}/approve")
    @RequireRole(Role.ADMIN)
    public BookingDto approve(@RequestAttribute("currentUser") User admin, @PathVariable Long id) {
        return bookings.approve(admin, id);
    }

    @PostMapping("/{id}/reject")
    @RequireRole(Role.ADMIN)
    public BookingDto reject(@RequestAttribute("currentUser") User admin, @PathVariable Long id,
                             @Valid @RequestBody(required = false) DecisionRequest body) {
        return bookings.reject(admin, id, body == null ? null : body.note());
    }

    @PostMapping("/{id}/cancel")
    public BookingDto cancel(@RequestAttribute("currentUser") User user, @PathVariable Long id) {
        return bookings.cancel(user, id);
    }
}
