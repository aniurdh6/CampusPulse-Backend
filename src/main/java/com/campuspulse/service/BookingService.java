package com.campuspulse.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuspulse.dto.Dtos.BookingDto;
import com.campuspulse.dto.Dtos.BookingRequest;
import com.campuspulse.exception.ApiException;
import com.campuspulse.model.Booking;
import com.campuspulse.model.BookingStatus;
import com.campuspulse.model.CampusResource;
import com.campuspulse.model.Role;
import com.campuspulse.model.User;
import com.campuspulse.repository.BookingRepository;
import com.campuspulse.repository.ResourceRepository;

@Service
public class BookingService {

    static final LocalTime OPENS = LocalTime.of(8, 0);
    static final LocalTime CLOSES = LocalTime.of(20, 0);

    private final BookingRepository bookings;
    private final ResourceRepository resources;
    private final NotificationService notifications;
    private final ActivityLogService logs;
    private final Clock clock;
    private final int maxHours;

    public BookingService(BookingRepository bookings, ResourceRepository resources,
                          NotificationService notifications, ActivityLogService logs, Clock clock,
                          @Value("${campuspulse.booking.max-hours:4}") int maxHours) {
        this.bookings = bookings;
        this.resources = resources;
        this.notifications = notifications;
        this.logs = logs;
        this.clock = clock;
        this.maxHours = maxHours;
    }

    @Transactional
    public BookingDto create(User user, BookingRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw ApiException.badRequest("End time must be after start time");
        }
        if (request.startTime().isBefore(OPENS) || request.endTime().isAfter(CLOSES)) {
            throw ApiException.badRequest("Bookings run between 08:00 and 20:00");
        }
        long minutes = Duration.between(request.startTime(), request.endTime()).toMinutes();
        if (minutes > maxHours * 60L) {
            throw ApiException.badRequest("A single booking can't be longer than " + maxHours + " hours");
        }
        if (LocalDateTime.of(request.date(), request.startTime()).isBefore(LocalDateTime.now(clock))) {
            throw ApiException.badRequest("That start time has already passed");
        }

        // Lock the resource row so the overlap check and the insert can't interleave with another booking.
        CampusResource resource = resources.findForUpdateById(request.resourceId())
                .orElseThrow(() -> ApiException.notFound("Resource not found"));
        if (!resource.isActive()) {
            throw ApiException.conflict(resource.getName() + " is out of service");
        }

        List<Booking> clashes = bookings.findOverlaps(resource.getId(), request.date(),
                request.startTime(), request.endTime(), BookingStatus.CONFIRMED);
        if (!clashes.isEmpty()) {
            Booking first = clashes.get(0);
            throw ApiException.conflict(resource.getName() + " is already booked from "
                    + first.getStartTime() + " to " + first.getEndTime() + " on that day");
        }

        Booking booking = new Booking();
        booking.setResource(resource);
        booking.setUser(user);
        booking.setBookingDate(request.date());
        booking.setStartTime(request.startTime());
        booking.setEndTime(request.endTime());
        booking.setPurpose(request.purpose() == null || request.purpose().isBlank() ? null : request.purpose().trim());
        // Admins book directly. Students and faculty send a request that an admin approves or rejects.
        boolean direct = user.getRole() == Role.ADMIN;
        booking.setStatus(direct ? BookingStatus.CONFIRMED : BookingStatus.PENDING);
        bookings.save(booking);

        String what = describe(booking);
        if (direct) {
            notifications.send(user, "Booking confirmed: " + what);
        } else {
            notifications.send(user, "Request sent. An admin will approve or reject it: " + what);
            notifications.sendToAdmins("New booking request from " + user.getName() + ": " + what);
        }
        logs.log(user.getName(), direct ? "BOOKING_CREATED" : "BOOKING_REQUESTED", what);
        return BookingDto.from(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingDto> mine(User user) {
        return bookings.findByUserIdOrderByBookingDateDescStartTimeDesc(user.getId()).stream()
                .map(BookingDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<BookingDto> all(LocalDate day) {
        List<Booking> found = day == null
                ? bookings.findAllByOrderByBookingDateDescStartTimeDesc()
                : bookings.findByBookingDateOrderByStartTime(day);
        return found.stream().map(BookingDto::from).toList();
    }

    @Transactional
    public BookingDto cancel(User actor, Long id) {
        Booking booking = bookings.findById(id).orElseThrow(() -> ApiException.notFound("Booking not found"));
        boolean owner = booking.getUser().getId().equals(actor.getId());
        if (!owner && actor.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("You can only cancel your own bookings");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED) {
            throw ApiException.conflict("This booking is already closed");
        }
        if (LocalDateTime.of(booking.getBookingDate(), booking.getEndTime()).isBefore(LocalDateTime.now(clock))) {
            throw ApiException.badRequest("Past bookings can't be cancelled");
        }
        booking.setStatus(BookingStatus.CANCELLED);

        String what = booking.getResource().getName() + " on " + booking.getBookingDate()
                + ", " + booking.getStartTime() + " to " + booking.getEndTime();
        if (!owner) {
            notifications.send(booking.getUser(), "An admin cancelled your booking: " + what);
        }
        logs.log(actor.getName(), "BOOKING_CANCELLED", what);
        return BookingDto.from(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingDto> pending() {
        return bookings.findByStatusOrderByBookingDateAscStartTimeAsc(BookingStatus.PENDING).stream()
                .map(BookingDto::from).toList();
    }

    @Transactional
    public BookingDto approve(User admin, Long id) {
        Booking booking = bookings.findById(id).orElseThrow(() -> ApiException.notFound("Booking not found"));
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw ApiException.conflict("Only pending requests can be approved");
        }
        if (LocalDateTime.of(booking.getBookingDate(), booking.getStartTime()).isBefore(LocalDateTime.now(clock))) {
            throw ApiException.badRequest("That start time has already passed. Reject the request instead");
        }

        // Same lock as create(), so two approvals for one slot can't both succeed.
        CampusResource resource = resources.findForUpdateById(booking.getResource().getId())
                .orElseThrow(() -> ApiException.notFound("Resource not found"));
        if (!resource.isActive()) {
            throw ApiException.conflict(resource.getName() + " is out of service");
        }
        List<Booking> clashes = bookings.findOverlaps(resource.getId(), booking.getBookingDate(),
                booking.getStartTime(), booking.getEndTime(), BookingStatus.CONFIRMED);
        if (!clashes.isEmpty()) {
            Booking first = clashes.get(0);
            throw ApiException.conflict("Can't approve: " + resource.getName() + " is already booked from "
                    + first.getStartTime() + " to " + first.getEndTime()
                    + ". Reject this request or cancel the other booking");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        notifications.send(booking.getUser(), "Approved: " + describe(booking));
        logs.log(admin.getName(), "BOOKING_APPROVED", booking.getUser().getName() + ", " + describe(booking));
        return BookingDto.from(booking);
    }

    @Transactional
    public BookingDto reject(User admin, Long id, String note) {
        Booking booking = bookings.findById(id).orElseThrow(() -> ApiException.notFound("Booking not found"));
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw ApiException.conflict("Only pending requests can be rejected");
        }
        String reason = note == null || note.isBlank() ? null : note.trim();
        booking.setStatus(BookingStatus.REJECTED);
        booking.setDecisionNote(reason);
        notifications.send(booking.getUser(), "Rejected: " + describe(booking) + (reason != null ? ". Reason: " + reason : ""));
        logs.log(admin.getName(), "BOOKING_REJECTED", booking.getUser().getName() + ", " + describe(booking));
        return BookingDto.from(booking);
    }

    private static String describe(Booking b) {
        return b.getResource().getName() + " on " + b.getBookingDate() + ", " + b.getStartTime() + " to " + b.getEndTime();
    }
}
