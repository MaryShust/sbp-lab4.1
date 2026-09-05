package com.example.sbp.controller;

import com.example.sbp.dto.BillCreateRequestDTO;
import com.example.sbp.dto.BillResponseDTO;
import com.example.sbp.exception.AccessDeniedException;
import com.example.sbp.listener.BillStatusListener;
import com.example.sbp.listener.ReplenishListener;
import com.example.sbp.security.SecurityService;
import com.example.sbp.service.BillService;
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
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/bills")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bills", description = "Управление счетами (Bill)")
public class BillController {

    private final BillService billService;
    private final SecurityService securityService;
    private final RuntimeService runtimeService;
    private final ReplenishListener replenishListener;
    private final BillStatusListener billStatusListener;

    @PostMapping
    @Operation(
            summary = "Создание нового счета",
            description = "Создает новый дополнительный счет (активный сразу). USER - только для своего аккаунта."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Счет успешно создан",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = """
                                    {
                                        "id": 2,
                                        "accountId": 1,
                                        "isActive": true,
                                        "status": "created"
                                    }
                                    """)))
    })
    @SneakyThrows
    public ResponseEntity<?> createBill(@Valid @RequestBody BillCreateRequestDTO billDTO) {
        Map<String, Object> variables = new HashMap<>();
        variables.putAll(securityService.getAuthVariables());
        variables.put("targetAccountId", billDTO.getAccountId());

        log.info("TEST 1");
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                    "bill-create-process", variables);

            String processInstanceId = processInstance.getId();

            log.info("TEST 2");
            Map<String, Object> resultVariables = billStatusListener
                    .waitForResult(processInstanceId)
                    .get(60, TimeUnit.SECONDS);

            log.info("TEST 3");
            BillResponseDTO response = new BillResponseDTO();
            response.setId((Long) resultVariables.get("id"));
            response.setAccountId((Long) resultVariables.get("accountId"));
            response.setBalance((BigDecimal) resultVariables.get("balance"));
            response.setIsActive((Boolean) resultVariables.get("isActive"));
            response.setCreatedAt((LocalDateTime) resultVariables.get("createdAt"));
            response.setUpdatedAt((LocalDateTime) resultVariables.get("updatedAt"));

            return ResponseEntity.ok(response);
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

    @SneakyThrows
    @GetMapping("/{id}")
    @Operation(
            summary = "Получить счет по ID",
            description = "Возвращает информацию о счете по его ID. USER - только свои счета, MANAGER - все счета."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Счет найден",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BillResponseDTO.class)))
    })
    public ResponseEntity<?> getBillById(
            @Parameter(description = "ID счета", example = "1")
            @PathVariable Long id) {

        // Подготовка переменных для Camunda процесса
        Map<String, Object> variables = new HashMap<>();
        variables.putAll(securityService.getAuthVariables());
        variables.put("billId", id);

        // Запуск Camunda процесса
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "get-bill-process", variables);

        String processInstanceId = processInstance.getId();

        // Ожидание результата через Execution Listener
        Map<String, Object> resultVariables = billStatusListener
                .waitForResult(processInstanceId)
                .get(30, TimeUnit.SECONDS);

        // Проверка успешности выполнения
        Boolean success = (Boolean) resultVariables.getOrDefault("success", false);
        if (!success) {
            String error = (String) resultVariables.get("error");
            throw new AccessDeniedException(error != null ? error : "Доступ запрещен");
        }

        // Формирование ответа из переменных процесса
        BillResponseDTO response = new BillResponseDTO();
        response.setId((Long) resultVariables.get("billId"));
        response.setAccountId((Long) resultVariables.get("accountId"));
        response.setBalance((BigDecimal) resultVariables.get("balance"));
        response.setIsActive((Boolean) resultVariables.get("isActive"));
        response.setCreatedAt((LocalDateTime) resultVariables.get("createdAt"));
        response.setUpdatedAt((LocalDateTime) resultVariables.get("updatedAt"));

        return ResponseEntity.ok(response);
    }

    @SneakyThrows
    @PostMapping("/{id}/replenish")
    @Operation(
            summary = "Пополнить счет",
            description = "Пополняет счет на указанную сумму. USER - только свои счета."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Счет успешно пополнен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BillResponseDTO.class)))
    })
    public ResponseEntity<?> replenishBill(
            @Parameter(description = "ID аккаунта", example = "1")
            @RequestParam Long accountId,

            @Parameter(description = "ID счета", example = "1")
            @PathVariable Long id,

            @Parameter(description = "Сумма пополнения", example = "1000.50")
            @RequestBody BigDecimal amount) {

        // Подготовка переменных для Camunda процесса
        Map<String, Object> variables = new HashMap<>();
        variables.putAll(securityService.getAuthVariables());
        variables.put("accountId", accountId);
        variables.put("billId", id);
        variables.put("amount", amount);
        log.info("TESTT BillController start: processKey={}, accountId={}, billId={}, amount={}",
                "bill-replenish-process", accountId, id, amount);

        // Запуск Camunda процесса
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "bill-replenish-process", variables);

        String processInstanceId = processInstance.getId();
        log.info("TESTT BillController process started: processInstanceId={}", processInstanceId);

        // Регистрируем future ПЕРЕД ожиданием (listener может сработать уже при asyncBefore)
        CompletableFuture<Map<String, Object>> future = replenishListener
                .waitForResult(processInstanceId);

        // Ожидание результата через Execution Listener
        Map<String, Object> resultVariables;
        try {
            resultVariables = future.get(30, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            log.warn("TESTT BillController timed out waiting for process result: processInstanceId={}", processInstanceId);
            throw new RuntimeException("Превышено время ожидания выполнения процесса", e);
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.info("TESTT BillController ExecutionException cause: {}", cause, cause);
            throw cause instanceof RuntimeException ? (RuntimeException) cause : new RuntimeException(e);
        }
        log.info("TESTT BillController result received: billId={}, newBalance={}",
                resultVariables.get("billId"), resultVariables.get("balance"));

        // Формирование ответа из переменных процесса
        BillResponseDTO response = new BillResponseDTO();
        response.setId((Long) resultVariables.get("billId"));
        response.setAccountId((Long) resultVariables.get("accountId"));
        response.setBalance((BigDecimal) resultVariables.get("balance"));
        response.setIsActive((Boolean) resultVariables.get("isActive"));
        response.setCreatedAt((LocalDateTime) resultVariables.get("createdAt"));
        response.setUpdatedAt((LocalDateTime) resultVariables.get("updatedAt"));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountId}/default")
    @Operation(
            summary = "Получить дефолтный счет аккаунта",
            description = "Возвращает дефолтный счет для указанного аккаунта. USER - только свои счета, MANAGER - все счета."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Дефолтный счет найден",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BillResponseDTO.class)))
    })
    public ResponseEntity<?> getDefaultBillByAccountId(
            @Parameter(description = "ID аккаунта", example = "1")
            @PathVariable Long accountId) {
        BillResponseDTO response = billService.getDefaultBillByAccountId(accountId);
        return ResponseEntity.ok(response);
    }
}