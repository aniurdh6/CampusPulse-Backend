package com.campuspulse.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campuspulse.model.ActivityLog;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findTop100ByOrderByCreatedAtDesc();
}
