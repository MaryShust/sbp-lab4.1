package com.example.sbp.controller;

import com.example.sbp.dto.PaymentRequestDTO;
import com.example.sbp.dto.PaymentResponseDTO;
import com.example.sbp.exception.AccessDeniedException;
import com.example.sbp.listener.PaymentStatusListener;
import com.example.sbp.security.SecurityService;
import com.example.sbp.service.PaymentService;
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
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Переводы по СБП")
public class PaymentController {

    private final SecurityService securityService;
    private final RuntimeService runtimeService;
    private final PaymentService paymentService;
    private final PaymentStatusListener paymentStatusListener;

    @PostMapping("/sbp")
    @Operation(
            summary = "Выполнить перевод по СБП",
            description = "Осуществляет перевод средств между счетами. USER - только со своих счетов."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Перевод успешно выполнен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaymentResponseDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                        "transactionId": "SBP1234567890_123",
                                        "status": "SUCCESS",
                                        "senderBillId": 1,
                                        "receiverBillId": 2,
                                        "amount": 1000.00,
                                        "commission": 10.00,
                                        "message": "Оплата услуг",
                                        "createdAt": "2024-01-01T12:00:00",
                                        "completedAt": "2024-01-01T12:00:01"
                                    }
                                    """)))
    })
    public ResponseEntity<?> processSbpPayment(@Valid @RequestBody PaymentRequestDTO request) {
        PaymentResponseDTO response = paymentService.processPayment(request);
        return ResponseEntity.ok(response);
    }

    @SneakyThrows
    @GetMapping("/{transactionId}/status")
    @Operation(
            summary = "Получить статус транзакции",
            description = "Возвращает информацию о транзакции по её ID. USER - только если участвует в транзакции, MANAGER - все."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Транзакция найдена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaymentResponseDTO.class)))
    })
    public ResponseEntity<PaymentResponseDTO> getPaymentStatus(
            @Parameter(description = "ID транзакции", example = "SBP1234567890_123")
            @PathVariable String transactionId
    ) {
        log.info("TEST: {}", transactionId);

        // Подготовка переменных
        Map<String, Object> variables = new HashMap<>();
        variables.putAll(securityService.getAuthVariables());
        variables.put("transactionId", transactionId);

        // Запуск процесса
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "payment-status-process", variables);

        String processInstanceId = processInstance.getId();
        log.info("TEST C: {}", processInstanceId);

        // Ждем результат через Execution Listener
        Map<String, Object> resultVariables = paymentStatusListener
                .waitForResult(processInstanceId)
                .get(30, TimeUnit.SECONDS);

        // Проверка успешности
        Boolean success = (Boolean) resultVariables.getOrDefault("success", false);
        if (!success) {
            String error = (String) resultVariables.get("error");
            throw new AccessDeniedException(error != null ? error : "Трунь");
        }

        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setTransactionId(transactionId);
        response.setStatus((String) resultVariables.get("status"));
        response.setSenderBillId((Long) resultVariables.get("senderBillId"));
        response.setReceiverBillId((Long) resultVariables.get("receiverBillId"));
        response.setAmount((BigDecimal) resultVariables.get("amount"));
        response.setCommission((BigDecimal) resultVariables.get("commission"));
        response.setMessage((String) resultVariables.get("message"));
        response.setCreatedAt((LocalDateTime) resultVariables.get("createdAt"));
        response.setCompletedAt((LocalDateTime) resultVariables.get("completedAt"));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(
            summary = "Проверка работоспособности",
            description = "Проверяет, что сервис работает. Доступен без аутентификации."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Сервис работает",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "status": "UP",
                                        "service": "SBP Payment Service"
                                    }
                                    """)))
    })
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "SBP Payment Service");
        return ResponseEntity.ok(response);
    }
}