package com.example.sbp.repository;

import com.example.sbp.entity.SbpTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SbpTransactionRepository extends JpaRepository<SbpTransactionEntity, Long> {
    Optional<SbpTransactionEntity> findByTransactionId(String transactionId);
}