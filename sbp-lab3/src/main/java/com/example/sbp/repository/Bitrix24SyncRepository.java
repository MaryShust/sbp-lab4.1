package com.example.sbp.repository;

import com.example.sbp.entity.Bitrix24SyncEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface Bitrix24SyncRepository extends JpaRepository<Bitrix24SyncEntity, Long> {
    Optional<Bitrix24SyncEntity> findBySuspicionId(Long suspicionId);
}
