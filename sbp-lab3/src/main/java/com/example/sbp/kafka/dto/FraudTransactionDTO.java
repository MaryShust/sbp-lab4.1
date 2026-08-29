package com.example.sbp.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudTransactionDTO {
    private String transactionId;
    private Long senderBillId;
    private Long receiverBillId;
    private String senderBankBic;
    private String receiverBankBic;
    private BigDecimal amount;
    private LocalDateTime createdAt;
}