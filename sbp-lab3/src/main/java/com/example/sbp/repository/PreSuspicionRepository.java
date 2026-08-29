package com.example.sbp.repository;

import com.example.sbp.entity.PreSuspicionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PreSuspicionRepository extends JpaRepository<PreSuspicionEntity, Long> {
    List<PreSuspicionEntity> findByEventTimeBetween(LocalDateTime start, LocalDateTime end);
}