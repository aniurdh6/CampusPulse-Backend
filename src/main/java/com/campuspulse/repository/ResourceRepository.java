package com.campuspulse.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.campuspulse.model.CampusResource;
import com.campuspulse.model.ResourceType;

import jakarta.persistence.LockModeType;

public interface ResourceRepository extends JpaRepository<CampusResource, Long> {

    List<CampusResource> findAllByOrderByNameAsc();

    List<CampusResource> findByTypeOrderByNameAsc(ResourceType type);

    long countByActiveTrue();

    /** Locks the row so two people can't book the same slot at the same moment. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CampusResource r where r.id = :id")
    Optional<CampusResource> findForUpdateById(@Param("id") Long id);
}
