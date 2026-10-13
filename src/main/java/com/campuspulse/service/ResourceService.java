package com.campuspulse.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuspulse.dto.Dtos.ResourceDto;
import com.campuspulse.dto.Dtos.ResourceRequest;
import com.campuspulse.dto.Dtos.SlotDto;
import com.campuspulse.exception.ApiException;
import com.campuspulse.model.Booking;
import com.campuspulse.model.BookingStatus;
import com.campuspulse.model.CampusResource;
import com.campuspulse.model.ResourceType;
import com.campuspulse.model.User;
import com.campuspulse.repository.BookingRepository;
import com.campuspulse.repository.MaintenanceRepository;
import com.campuspulse.repository.ResourceRepository;

@Service
public class ResourceService {

    private final ResourceRepository resources;
    private final BookingRepository bookings;
    private final MaintenanceRepository maintenance;
    private final ActivityLogService logs;

    public ResourceService(ResourceRepository resources, BookingRepository bookings,
                           MaintenanceRepository maintenance, ActivityLogService logs) {
        this.resources = resources;
        this.bookings = bookings;
        this.maintenance = maintenance;
        this.logs = logs;
    }

    @Transactional(readOnly = true)
    public List<ResourceDto> list(ResourceType type) {
        List<CampusResource> found = type == null
                ? resources.findAllByOrderByNameAsc()
                : resources.findByTypeOrderByNameAsc(type);
        return found.stream().map(ResourceDto::from).toList();
    }

    @Transactional(readOnly = true)
    public ResourceDto get(Long id) {
        return ResourceDto.from(find(id));
    }

    /** Confirmed bookings for one day, grouped by resource id, for drawing availability timelines. */
    @Transactional(readOnly = true)
    public Map<Long, List<SlotDto>> availability(LocalDate day) {
        Map<Long, List<SlotDto>> byResource = new HashMap<>();
        List<BookingStatus> shown = List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING);
        for (Booking b : bookings.findByBookingDateAndStatusInOrderByStartTime(day, shown)) {
            byResource.computeIfAbsent(b.getResource().getId(), k -> new ArrayList<>())
                    .add(new SlotDto(b.getStartTime(), b.getEndTime(), b.getUser().getName(), b.getStatus()));
        }
        return byResource;
    }

    @Transactional
    public ResourceDto create(User admin, ResourceRequest request) {
        CampusResource resource = new CampusResource();
        apply(resource, request);
        resources.save(resource);
        logs.log(admin.getName(), "RESOURCE_CREATED", resource.getName());
        return ResourceDto.from(resource);
    }

    @Transactional
    public ResourceDto update(User admin, Long id, ResourceRequest request) {
        CampusResource resource = find(id);
        apply(resource, request);
        logs.log(admin.getName(), "RESOURCE_UPDATED", resource.getName());
        return ResourceDto.from(resource);
    }

    @Transactional
    public void delete(User admin, Long id) {
        CampusResource resource = find(id);
        if (bookings.existsByResourceId(id) || maintenance.existsByResourceId(id)) {
            throw ApiException.conflict("This resource has bookings or issues on record. Mark it inactive instead of deleting it");
        }
        resources.delete(resource);
        logs.log(admin.getName(), "RESOURCE_DELETED", resource.getName());
    }

    private CampusResource find(Long id) {
        return resources.findById(id).orElseThrow(() -> ApiException.notFound("Resource not found"));
    }

    private void apply(CampusResource resource, ResourceRequest request) {
        resource.setName(request.name().trim());
        resource.setType(request.type());
        resource.setLocation(request.location().trim());
        resource.setCapacity(request.capacity());
        resource.setFeatures(normalizeFeatures(request.features()));
        resource.setDescription(request.description() == null || request.description().isBlank()
                ? null : request.description().trim());
        resource.setActive(request.active() == null || request.active());
    }

    private static String normalizeFeatures(String raw) {
        if (raw == null) {
            return "";
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.joining(","));
    }
}
