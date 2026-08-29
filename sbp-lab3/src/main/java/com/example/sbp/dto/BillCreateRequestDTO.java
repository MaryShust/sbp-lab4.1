package com.example.sbp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Запрос на создание счета")
public class BillCreateRequestDTO {

    @Schema(description = "ID аккаунта владельца",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ID аккаунта обязателен")
    private Long accountId;
}