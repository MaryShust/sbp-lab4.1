package com.example.sbp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "Ответ на перевод по СБП")
public class PaymentResponseDTO {

    @Schema(description = "Уникальный ID транзакции", example = "SBP1234567890_123")
    private String transactionId;

    @Schema(description = "Статус транзакции", example = "SUCCESS")
    private String status;

    @Schema(description = "ID счета отправителя", example = "1")
    private Long senderBillId;

    @Schema(description = "ID счета получателя", example = "2")
    private Long receiverBillId;

    @Schema(description = "Сумма перевода", example = "1000.00")
    private BigDecimal amount;

    @Schema(description = "Комиссия", example = "10.00")
    private BigDecimal commission;

    @Schema(description = "Сообщение", example = "Оплата услуг")
    private String message;

    @Schema(description = "Дата создания", example = "2024-01-01T12:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Дата завершения", example = "2024-01-01T12:00:01")
    private LocalDateTime completedAt;
}