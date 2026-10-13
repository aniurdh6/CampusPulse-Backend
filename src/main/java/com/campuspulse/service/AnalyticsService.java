package com.campuspulse.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuspulse.dto.Dtos.AnalyticsDto;
import com.campuspulse.dto.Dtos.DayCount;
import com.campuspulse.dto.Dtos.NameCount;
import com.campuspulse.model.Booking;
import com.campuspulse.model.BookingStatus;
import com.campuspulse.model.MaintenanceRequest;
import com.campuspulse.model.MaintenanceStatus;
import com.campuspulse.model.Priority;
import com.campuspulse.model.ResourceType;
import com.campuspulse.repository.BookingRepository;
import com.campuspulse.repository.MaintenanceRepository;
import com.campuspulse.repository.ResourceRepository;

@Service
public class AnalyticsService {

    /** Bookable hours in a day (08:00 to 20:00), used for the utilization figure. */
    private static final int OPEN_MINUTES = 12 * 60;

    private final ResourceRepository resources;
    private final BookingRepository bookings;
    private final MaintenanceRepository maintenance;
    private final Clock clock;

    public AnalyticsService(ResourceRepository resources, BookingRepository bookings,
                            MaintenanceRepository maintenance, Clock clock) {
        this.resources = resources;
        this.bookings = bookings;
        this.maintenance = maintenance;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AnalyticsDto summary() {
        LocalDate today = LocalDate.now(clock);
        long activeResources = resources.countByActiveTrue();

        List<Booking> todays = bookings.findByBookingDateAndStatusOrderByStartTime(today, BookingStatus.CONFIRMED);
        long bookedMinutes = todays.stream()
                .mapToLong(b -> Duration.between(b.getStartTime(), b.getEndTime()).toMinutes())
                .sum();
        double capacityMinutes = (double) activeResources * OPEN_MINUTES;
        double utilization = capacityMinutes == 0 ? 0 : round1(bookedMinutes * 100.0 / capacityMinutes);

        Map<String, Long> byType = new LinkedHashMap<>();
        for (ResourceType t : ResourceType.values()) {
            byType.put(t.name(), 0L);
        }
        for (Object[] row : bookings.countByResourceType(BookingStatus.CONFIRMED)) {
            byType.put(((ResourceType) row[0]).name(), (Long) row[1]);
        }

        List<NameCount> top = new ArrayList<>();
        for (Object[] row : bookings.topResources(BookingStatus.CONFIRMED, PageRequest.of(0, 5))) {
            top.add(new NameCount((String) row[0], (Long) row[1]));
        }

        Map<String, Long> issuesByStatus = new LinkedHashMap<>();
        for (MaintenanceStatus s : MaintenanceStatus.values()) {
            issuesByStatus.put(s.name(), 0L);
        }
        for (Object[] row : maintenance.countByStatus()) {
            issuesByStatus.put(((MaintenanceStatus) row[0]).name(), (Long) row[1]);
        }

        Map<String, Long> openByPriority = new LinkedHashMap<>();
        for (Priority p : Priority.values()) {
            openByPriority.put(p.name(), 0L);
        }
        for (Object[] row : maintenance.countOpenByPriority(MaintenanceStatus.RESOLVED)) {
            openByPriority.put(((Priority) row[0]).name(), (Long) row[1]);
        }

        LocalDate from = today.minusDays(6);
        Map<LocalDate, Long> perDay = bookings.findByBookingDateBetweenAndStatus(from, today, BookingStatus.CONFIRMED)
                .stream().collect(Collectors.groupingBy(Booking::getBookingDate, Collectors.counting()));
        List<DayCount> lastWeek = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = from.plusDays(i);
            lastWeek.add(new DayCount(d, perDay.getOrDefault(d, 0L)));
        }

        List<MaintenanceRequest> resolved = maintenance.findByStatus(MaintenanceStatus.RESOLVED);
        double avgHours = resolved.isEmpty() ? 0 : round1(resolved.stream()
                .mapToLong(m -> Duration.between(m.getCreatedAt(), m.getUpdatedAt()).toMinutes())
                .average().orElse(0) / 60.0);

        return new AnalyticsDto(resources.count(), activeResources, bookings.countByStatus(BookingStatus.CONFIRMED),
                todays.size(), maintenance.countByStatusNot(MaintenanceStatus.RESOLVED), utilization, avgHours,
                byType, top, issuesByStatus, openByPriority, lastWeek);
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
