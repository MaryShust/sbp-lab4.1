package com.example.sbp.controller;

import com.example.sbp.dto.BankAccountRequestDTO;
import com.example.sbp.dto.BankAccountResponseDTO;
import com.example.sbp.listener.AccountActiveListener;
import com.example.sbp.listener.AccountCreateListener;
import com.example.sbp.listener.AccountStatusListener;
import com.example.sbp.security.SecurityService;
import com.example.sbp.service.BankAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Accounts", description = "Управление аккаунтами пользователей")
public class BankAccountController {

    private final BankAccountService bankAccountService;
    private final SecurityService securityService;
    private final RuntimeService runtimeService;
    private final AccountStatusListener accountStatusListener;
    private final AccountActiveListener accountActiveListener;
    private final AccountCreateListener accountCreateListener;

    @PostMapping
    @PreAuthorize("hasAuthority('ACCOUNT_CREATE')")
    @Operation(
            summary = "Создание нового аккаунта",
            description = "Создает новый аккаунт и дефолтный счет (неактивный). Только для MANAGER в офисе банка."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Аккаунт успешно создан",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = """
                                    {
                                        "id": 1,
                                        "phoneNumber": "79123456789",
                                        "defaultBillId": 1,
                                        "status": "created",
                                        "message": "Account created. Default bill is inactive - please fund it to activate"
                                    }
                                    """)))
    })
    @SneakyThrows
    public ResponseEntity<?> createAccount(@Valid @RequestBody BankAccountRequestDTO bankAccountRequestDTO) {
        Map<String, Object> variables = new HashMap<>();
        variables.putAll(securityService.getAuthVariables());
        variables.put("phoneNumber", bankAccountRequestDTO.getPhoneNumber());
        variables.put("ownerName", bankAccountRequestDTO.getOwnerName());
        variables.put("bankBic", bankAccountRequestDTO.getBankBic());

        log.info("test 1");
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                    "account-create-process", variables);

            String processInstanceId = processInstance.getId();

            log.info("test 2");
            Map<String, Object> resultVariables = accountCreateListener
                    .waitForResult(processInstanceId)
                    .get(60, TimeUnit.SECONDS);

            log.info("test 3");
            Map<String, Object> result = new HashMap<>();
            result.put("id", resultVariables.get("id"));
            result.put("phoneNumber", resultVariables.get("createdPhoneNumber"));
            result.put("defaultBillId", resultVariables.get("defaultBillId"));
            result.put("status", "created");
            result.put("message", "Счет создан. Пополните для активации");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.info("test + " + e.getMessage());
            log.info("test + " + (e.getCause() instanceof BpmnError));
            if (e.getCause() instanceof BpmnError) {
                BpmnError bpmnError = (BpmnError) e.getCause();
                throw bpmnError;
            }
            throw new RuntimeException("Ошибка выполнения процесса", e);
        }
    }

    @SneakyThrows
    @GetMapping("/{id}")
    @Operation(
            summary = "Получить аккаунт по ID",
            description = "Возвращает информацию об аккаунте по его ID. USER - только свой аккаунт, MANAGER - любой."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Аккаунт найден",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BankAccountResponseDTO.class))),
    })
    public ResponseEntity<?> getAccountById(
            @Parameter(description = "ID аккаунта", example = "1")
            @PathVariable Long id
    ) {
        // Подготовка переменных
        Map<String, Object> variables = new HashMap<>();
        variables.putAll(securityService.getAuthVariables());
        variables.put("id", id);

        log.info("test 1");
        // Запуск процесса
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                    "account-info-process", variables);

            String processInstanceId = processInstance.getId();

            log.info("test 2");
            Map<String, Object> resultVariables = accountStatusListener
                    .waitForResult(processInstanceId)
                    .get(120, TimeUnit.SECONDS);

            log.info("test 3");
            BankAccountResponseDTO response = buildResponse(resultVariables);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.info("test + " + e.getMessage());
            log.info("test + " + (e.getCause() instanceof BpmnError));
            if (e.getCause() instanceof BpmnError) {
                BpmnError bpmnError = (BpmnError) e.getCause();
                throw bpmnError;
            }
            throw new RuntimeException("Ошибка выполнения процесса", e);
        }
    }

    private BankAccountResponseDTO buildResponse(Map<String, Object> variables) {
        BankAccountResponseDTO response = new BankAccountResponseDTO();
        response.setId((Long) variables.get("id"));
        response.setPhoneNumber((String) variables.get("phoneNumber"));
        response.setOwnerName((String) variables.get("ownerName"));
        response.setBankBic((String) variables.get("bankBic"));
        response.setIsActive((Boolean) variables.get("isActive"));
        response.setCreatedAt((LocalDateTime) variables.get("createdAt"));
        response.setUpdatedAt((LocalDateTime) variables.get("updatedAt"));
        response.setDefaultBillId((Long) variables.get("defaultBillId"));
        response.setAllBillIds((List<Long>) variables.get("allBillIds"));
        return response;
    }

    @GetMapping("/phone/{phoneNumber}")
    @Operation(
            summary = "Получить аккаунт по номеру телефона",
            description = "Возвращает информацию об аккаунте по номеру телефона. USER - только свой аккаунт, MANAGER - любой."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Аккаунт найден",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BankAccountResponseDTO.class)))
    })
    public ResponseEntity<?> getAccountByPhone(
            @Parameter(description = "Номер телефона", example = "79123456789")
            @PathVariable String phoneNumber
    ) {
        BankAccountResponseDTO response = bankAccountService.getAccountByPhone(phoneNumber);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountId}/activate-default")
    @Operation(
            summary = "Активировать дефолтный счет",
            description = "Пополняет и активирует дефолтный счет аккаунта. USER - только свой аккаунт."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Счет успешно активирован",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "message": "Default bill activated successfully"
                                    }
                                    """)))
    })
    @SneakyThrows
    public ResponseEntity<?> activateDefaultBill(
            @Parameter(description = "ID аккаунта", example = "1")
            @PathVariable Long accountId,
            @Parameter(description = "Начальный баланс для активации", example = "1000")
            @RequestParam(required = true) BigDecimal startBalance
    ) {
        Map<String, Object> variables = new HashMap<>();
        variables.putAll(securityService.getAuthVariables());
        variables.put("targetAccountId", accountId);
        variables.put("startBalance", startBalance);

        log.info("test 1");
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                    "account-activate-process", variables);

            String processInstanceId = processInstance.getId();

            log.info("test 2");
            accountActiveListener
                    .waitForResult(processInstanceId)
                    .get(60, TimeUnit.SECONDS);

            log.info("test 3");
            Map<String, String> response = new HashMap<>();
            response.put("message", "Дефолтный счет активирован");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.info("test + " + e.getMessage());
            log.info("test + " + (e.getCause() instanceof BpmnError));
            if (e.getCause() instanceof BpmnError) {
                BpmnError bpmnError = (BpmnError) e.getCause();
                throw bpmnError;
            }
            throw new RuntimeException("Ошибка выполнения процесса", e);
        }
    }
}