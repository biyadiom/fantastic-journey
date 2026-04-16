package com.fantastic.springai.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fantastic.springai.model.ActivityLog;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findByUser_IdOrderByLoggedAtDesc(Long userId, Pageable pageable);

    List<ActivityLog> findByEntity(String entity);

    List<ActivityLog> findByLoggedAtBetween(LocalDateTime start, LocalDateTime end);
}
