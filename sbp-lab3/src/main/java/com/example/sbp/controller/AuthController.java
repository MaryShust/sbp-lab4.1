package com.example.sbp.controller;

import com.example.sbp.dto.LoginRequestDTO;
import com.example.sbp.dto.RegisterRequestDTO;
import com.example.sbp.dto.UpdateRoleRequestDTO;
import com.example.sbp.dto.UserResponseDTO;
import com.example.sbp.listener.AccountStatusListener;
import com.example.sbp.security.SecurityService;
import com.example.sbp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "API для аутентификации и управления пользователями")
public class AuthController {

    private final AuthService authService;
    private final SecurityService securityService;
    private final RuntimeService runtimeService;
    private final AccountStatusListener accountStatusListener;

    @PostMapping("/login")
    @Operation(summary = "Аутентификация пользователя", description = "Аутентификация пользователя и возвращает JWT-токен. Предыдущие токены становятся недействительными.")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        String jwt = authService.login(
                loginRequest.getUsername(),
                loginRequest.getPassword()
        );
        return ResponseEntity.ok(Map.of("token", jwt));
    }

    @PostMapping("/register")
    @Operation(summary = "Регистрация нового пользователя", description = "Создаёт нового пользователя с указанными данными и возвращает JWT-токен.")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO registerRequest) {
        String jwt = authService.register(
                registerRequest.getUsername(),
                registerRequest.getPassword(),
                registerRequest.getPhoneNumber()
        );
        return ResponseEntity.ok(Map.of("token", jwt));
    }

    @GetMapping("/users/roles")
    @Operation(summary = "Получить роль текущего пользователя", description = "Возвращает роль текущего пользователя.")
    public ResponseEntity<UserResponseDTO> getCurrentUserRole() {
        Map<String, Object> variables = new HashMap<>();
        variables.putAll(securityService.getAuthVariables());

        log.info("TEST 1");
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                    "user-role-get-process", variables);

            String processInstanceId = processInstance.getId();

            log.info("TEST 2");
            Map<String, Object> resultVariables = accountStatusListener
                    .waitForResult(processInstanceId)
                    .get(60, TimeUnit.SECONDS);

            log.info("TEST 3");
            return ResponseEntity.ok(new UserResponseDTO(
                    (String) resultVariables.get("username"),
                    (String) resultVariables.get("role")
            ));
        } catch (Exception e) {
            log.info("TEST + " + e.getMessage());
            log.info("TEST + " + (e.getCause() instanceof BpmnError));
            if (e.getCause() instanceof BpmnError) {
                BpmnError bpmnError = (BpmnError) e.getCause();
                throw bpmnError;
            }
            throw new RuntimeException("Ошибка выполнения процесса", e);
        }
    }

    @GetMapping("/users/{username}")
    @PreAuthorize("hasAuthority('USER_MANAGE_ROLES')")
    @Operation(summary = "Получить информацию по пользователю", description = "Возвращает информацию по имени (только у админа)")
    public ResponseEntity<UserResponseDTO> getUser(@PathVariable String username) {
        UserResponseDTO user = authService.getUser(username);
        return ResponseEntity.ok(user);
    }

    @SneakyThrows
    @PutMapping("/users/roles")
    @PreAuthorize("hasAuthority('USER_MANAGE_ROLES')")
    @Operation(summary = "Обновляет роль пользователя", description = "Обновляет роль для конкретного пользователя. Все токены пользователя становятся недействительными.")
    public ResponseEntity<UserResponseDTO> updateUserRoles(@Valid @RequestBody UpdateRoleRequestDTO request) {
        Map<String, Object> variables = new HashMap<>();
        variables.putAll(securityService.getAuthVariables());
        variables.put("targetUsername", request.getUsername());
        variables.put("role", request.getRole());

        log.info("TEST 1");
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                    "user-role-update-process", variables);

            String processInstanceId = processInstance.getId();

            log.info("TEST 2");
            Map<String, Object> resultVariables = accountStatusListener
                    .waitForResult(processInstanceId)
                    .get(60, TimeUnit.SECONDS);

            log.info("TEST 3");
            return ResponseEntity.ok(new UserResponseDTO(
                    (String) resultVariables.get("username"),
                    (String) resultVariables.get("role")
            ));
        } catch (Exception e) {
            log.info("TEST + " + e.getMessage());
            log.info("TEST + " + (e.getCause() instanceof BpmnError));
            if (e.getCause() instanceof BpmnError) {
                BpmnError bpmnError = (BpmnError) e.getCause();
                throw bpmnError;
            }
            throw new RuntimeException("Ошибка выполнения процесса", e);
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Разлогин, аннулировать все токены пользователя", description = "Делает все текущие токены пользователя недействительными.")
    public ResponseEntity<Map<String, Object>> logout(@PathVariable String username) {
        int newTokenVersion = authService.logout(username);
        return ResponseEntity.ok(Map.of(
                "username", username,
                "tokenVersion", newTokenVersion
        ));
    }
}