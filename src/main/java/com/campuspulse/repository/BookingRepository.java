package com.campuspulse.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.campuspulse.model.Booking;
import com.campuspulse.model.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("select b from Booking b where b.resource.id = :resourceId and b.bookingDate = :day "
            + "and b.status = :status and b.startTime < :slotEnd and b.endTime > :slotStart "
            + "order by b.startTime")
    List<Booking> findOverlaps(@Param("resourceId") Long resourceId,
                               @Param("day") LocalDate day,
                               @Param("slotStart") LocalTime slotStart,
                               @Param("slotEnd") LocalTime slotEnd,
                               @Param("status") BookingStatus status);

    @Query("select b from Booking b where b.bookingDate = :day and b.status = :status "
            + "and b.startTime < :slotEnd and b.endTime > :slotStart")
    List<Booking> findAllOverlapping(@Param("day") LocalDate day,
                                     @Param("slotStart") LocalTime slotStart,
                                     @Param("slotEnd") LocalTime slotEnd,
                                     @Param("status") BookingStatus status);

    List<Booking> findByBookingDateAndStatusOrderByStartTime(LocalDate day, BookingStatus status);

    List<Booking> findByBookingDateAndStatusInOrderByStartTime(LocalDate day, Collection<BookingStatus> statuses);

    List<Booking> findByStatusOrderByBookingDateAscStartTimeAsc(BookingStatus status);

    List<Booking> findByUserIdOrderByBookingDateDescStartTimeDesc(Long userId);

    List<Booking> findAllByOrderByBookingDateDescStartTimeDesc();

    List<Booking> findByBookingDateOrderByStartTime(LocalDate day);

    List<Booking> findByBookingDateBetweenAndStatus(LocalDate from, LocalDate to, BookingStatus status);

    boolean existsByResourceId(Long resourceId);

    long countByStatus(BookingStatus status);

    @Query("select b.resource.name, count(b) from Booking b where b.status = :status "
            + "group by b.resource.name order by count(b) desc")
    List<Object[]> topResources(@Param("status") BookingStatus status, Pageable pageable);

    @Query("select b.resource.type, count(b) from Booking b where b.status = :status group by b.resource.type")
    List<Object[]> countByResourceType(@Param("status") BookingStatus status);
}
