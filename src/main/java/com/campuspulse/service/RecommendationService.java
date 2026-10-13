package com.campuspulse.service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuspulse.dto.Dtos.RecommendationDto;
import com.campuspulse.dto.Dtos.RecommendationRequest;
import com.campuspulse.dto.Dtos.ResourceDto;
import com.campuspulse.exception.ApiException;
import com.campuspulse.model.Booking;
import com.campuspulse.model.BookingStatus;
import com.campuspulse.model.CampusResource;
import com.campuspulse.repository.BookingRepository;
import com.campuspulse.repository.ResourceRepository;

/**
 * Rule based matching. Only resources that are active, free for the whole slot and big enough
 * are suggested. They are scored on how many requested features they have (40 points) and how
 * snugly they fit the group size (20 points), on top of 40 for being free.
 */
@Service
public class RecommendationService {

    private static final int MAX_RESULTS = 8;

    private final ResourceRepository resources;
    private final BookingRepository bookings;

    public RecommendationService(ResourceRepository resources, BookingRepository bookings) {
        this.resources = resources;
        this.bookings = bookings;
    }

    @Transactional(readOnly = true)
    public List<RecommendationDto> recommend(RecommendationRequest request) {
        LocalTime start = request.startTime();
        LocalTime end = request.endTime();
        if (!end.isAfter(start)) {
            throw ApiException.badRequest("End time must be after start time");
        }

        Set<Long> busy = new HashSet<>();
        for (Booking b : bookings.findAllOverlapping(request.date(), start, end, BookingStatus.CONFIRMED)) {
            busy.add(b.getResource().getId());
        }

        List<String> wanted = request.features() == null ? List.of() : request.features().stream()
                .map(f -> f.trim().toLowerCase(Locale.ROOT))
                .filter(f -> !f.isEmpty())
                .distinct()
                .toList();

        List<CampusResource> candidates = request.type() == null
                ? resources.findAllByOrderByNameAsc()
                : resources.findByTypeOrderByNameAsc(request.type());

        List<RecommendationDto> results = new ArrayList<>();
        for (CampusResource r : candidates) {
            if (!r.isActive() || busy.contains(r.getId())) {
                continue;
            }
            Integer minCapacity = request.minCapacity();
            if (minCapacity != null && r.getCapacity() < minCapacity) {
                continue;
            }

            List<String> reasons = new ArrayList<>();
            reasons.add("Free " + start + " to " + end);

            Set<String> have = r.featureList().stream()
                    .map(f -> f.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            double featureScore = 40;
            if (!wanted.isEmpty()) {
                List<String> matched = wanted.stream().filter(have::contains).toList();
                List<String> missing = wanted.stream().filter(f -> !have.contains(f)).toList();
                featureScore = 40.0 * matched.size() / wanted.size();
                if (!matched.isEmpty()) {
                    reasons.add("Has " + String.join(", ", matched));
                }
                if (!missing.isEmpty()) {
                    reasons.add("Missing " + String.join(", ", missing));
                }
            }

            double fitScore = 20;
            if (minCapacity != null) {
                fitScore = 20.0 * minCapacity / r.getCapacity();
                reasons.add("Seats " + r.getCapacity() + " (you need " + minCapacity + ")");
            }

            int score = (int) Math.round(40 + featureScore + fitScore);
            results.add(new RecommendationDto(ResourceDto.from(r), score, reasons));
        }

        results.sort(Comparator.comparingInt(RecommendationDto::score).reversed()
                .thenComparing(d -> d.resource().name()));
        return results.size() > MAX_RESULTS ? results.subList(0, MAX_RESULTS) : results;
    }
}
