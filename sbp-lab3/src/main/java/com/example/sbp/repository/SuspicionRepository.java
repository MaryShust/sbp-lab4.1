package com.example.sbp.repository;

import com.example.sbp.entity.SuspicionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SuspicionRepository extends JpaRepository<SuspicionEntity, Long> {
    @Query("SELECT s FROM SuspicionEntity s WHERE s.userName = :userName " +
           "AND s.accountId = :accountId AND s.bankBic = :bankBic " +
           "AND s.analysisDate >= :since")
    Optional<SuspicionEntity> findRecentByUserAndAccountAndBank(
            @Param("userName") String userName,
            @Param("accountId") Long accountId,
            @Param("bankBic") String bankBic,
            @Param("since") LocalDateTime since);

    @Modifying
    @Transactional
    @Query("UPDATE SuspicionEntity s SET s.duplicateCount = :count WHERE s.id = :id")
    void updateDuplicateCount(@Param("id") Long id, @Param("count") Integer count);
}