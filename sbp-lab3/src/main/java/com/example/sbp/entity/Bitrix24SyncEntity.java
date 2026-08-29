package com.example.sbp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bitrix24_sync")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bitrix24SyncEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "suspicion_id", nullable = false, unique = true)
    private Long suspicionId;

    @Column(name = "bitrix24_deal_id")
    private Long bitrix24DealId;

    @Column(name = "sync_status", nullable = false, length = 20)
    private String syncStatus;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        syncedAt = LocalDateTime.now();
    }

    public enum SyncStatus {
        PENDING, SUCCESS, FAILED
    }
}
