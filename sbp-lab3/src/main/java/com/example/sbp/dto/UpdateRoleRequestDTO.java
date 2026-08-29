package com.example.sbp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на изменение ролей пользователя")
public class UpdateRoleRequestDTO {

    @NotBlank(message = "Имя пользователя обязательно")
    @Schema(description = "Имя пользователя", example = "user123")
    private String username;

    @NotEmpty(message = "Роль обязательна")
    @Schema(description = "Роль", example = "USER")
    private String role;
}
