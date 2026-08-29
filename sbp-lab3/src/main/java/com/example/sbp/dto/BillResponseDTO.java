package com.example.sbp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "Ответ с данными счета")
public class BillResponseDTO {

    @Schema(description = "ID счета", example = "1")
    private Long id;

    @Schema(description = "ID аккаунта владельца", example = "1")
    private Long accountId;

    @Schema(description = "Баланс", example = "1500.50")
    private BigDecimal balance;

    @Schema(description = "Активен ли счет", example = "true")
    private Boolean isActive;

    @Schema(description = "Является ли дефолтным", example = "false")
    private Boolean isDefault;

    @Schema(description = "Дата создания", example = "2024-01-01T12:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Дата обновления", example = "2024-01-01T12:00:00")
    private LocalDateTime updatedAt;
}