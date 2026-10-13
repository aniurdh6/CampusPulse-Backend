package com.campuspulse.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.campuspulse.model.MaintenanceRequest;
import com.campuspulse.model.MaintenanceStatus;

public interface MaintenanceRepository extends JpaRepository<MaintenanceRequest, Long> {

    List<MaintenanceRequest> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    List<MaintenanceRequest> findAllByOrderByCreatedAtDesc();

    List<MaintenanceRequest> findByStatusOrderByCreatedAtDesc(MaintenanceStatus status);

    List<MaintenanceRequest> findByStatus(MaintenanceStatus status);

    boolean existsByResourceId(Long resourceId);

    long countByStatusNot(MaintenanceStatus status);

    @Query("select m.status, count(m) from MaintenanceRequest m group by m.status")
    List<Object[]> countByStatus();

    @Query("select m.priority, count(m) from MaintenanceRequest m where m.status <> :done group by m.priority")
    List<Object[]> countOpenByPriority(@Param("done") MaintenanceStatus done);
}
