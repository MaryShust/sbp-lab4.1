package com.example.sbp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Запрос на создание аккаунта")
public class BankAccountRequestDTO {

    @Schema(
            description = "Номер телефона в формате 7XXXXXXXXXX",
            example = "79123456789",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Телефон обязателен")
    @Pattern(regexp = "^7[0-9]{10}$", message = "Телефон: 79991234567")
    private String phoneNumber;

    @Schema(
            description = "Имя владельца",
            example = "Иван Петров",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Имя обязательно")
    private String ownerName;

    @Schema(
            description = "БИК банка",
            example = "044525555",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "БИК обязателен")
    @Pattern(regexp = "^[0-9]{9}$", message = "БИК: 9 цифр")
    private String bankBic;
}