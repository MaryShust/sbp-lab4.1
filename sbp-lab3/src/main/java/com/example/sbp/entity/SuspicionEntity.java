package com.example.sbp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "suspicion")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuspicionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name", nullable = false, length = 100)
    @Size(max = 100, message = "Имя владельца не должно превышать 100 символов")
    private String userName;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "bank_bic", nullable = false, length = 11)
    @Size(min = 8, max = 11, message = "Код BIC банка должен содержать от 8 до 11 символов")
    private String bankBic;

    @Column(name = "duplicate_count", nullable = false)
    private Integer duplicateCount;

    @Column(name = "analysis_date", nullable = false)
    private LocalDateTime analysisDate;

    @Column(name = "sender_account_id")
    private Long senderAccountId;

    @Column(name = "sender_bank_bic", length = 11)
    private String senderBankBic;
}
