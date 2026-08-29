package com.example.sbp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Информация о пользователе")
public class UserResponseDTO {

    @Schema(description = "Имя пользователя", example = "user123")
    private String username;

    @Schema(description = "Роль пользователя", example = "USER")
    private String role;
}
