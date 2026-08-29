package com.example.sbp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Запрос на аутентификацию")
public class LoginRequestDTO {

    @NotBlank(message = "Имя пользователя обязательно")
    @Schema(
            description = "Имя пользователя",
            example = "user",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String username;

    @NotBlank(message = "Пароль обязателен")
    @Schema(
            description = "Пароль",
            example = "password123",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String password;
}