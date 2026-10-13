package com.campuspulse.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campuspulse.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop100ByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndSeenFalse(Long userId);

    long countByUserIdAndSeenFalse(Long userId);
}
