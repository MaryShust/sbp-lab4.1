package com.example.sbp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "Запрос на перевод по СБП")
public class PaymentRequestDTO {

    @Schema(
            description = "ID счета отправителя",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "ID счета отправителя обязателен")
    private Long senderBillId;

    @Schema(
            description = "Идентификатор получателя (ID счета или номер телефона)",
            example = "79234567890",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Идентификатор получателя обязателен")
    private String receiverIdentifier; // может быть ID счета или номер телефона

    @Schema(
            description = "Сумма перевода",
            example = "1000.00",
            minimum = "0.01",
            maximum = "1000000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Сумма обязательна")
    @DecimalMin(value = "0.01", message = "Минимум 0.01")
    @DecimalMax(value = "1000000", message = "Максимум 1 000 000")
    private BigDecimal amount;

    @Schema(
            description = "Сообщение к переводу",
            example = "Оплата услуг",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String message;
}