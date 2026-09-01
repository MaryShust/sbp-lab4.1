package com.example.sbp.controller;

import com.example.sbp.dto.PaymentRequestDTO;
import com.example.sbp.dto.PaymentResponseDTO;
import com.example.sbp.exception.AccessDeniedException;
import com.example.sbp.listener.PaymentListener;
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
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.delegate.BpmnError;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Переводы по СБП")
public class PaymentController {

    private final SecurityService securityService;
    private final RuntimeService runtimeService;
    private final PaymentStatusListener paymentStatusListener;
    private final PaymentListener paymentListener;

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
        // Подготовка переменных
        Map<String, Object> variables = new HashMap<>();
        variables.putAll(securityService.getAuthVariables());
        variables.put("senderBillId", request.getSenderBillId());
        variables.put("receiverIdentifier", request.getReceiverIdentifier());
        variables.put("amount", request.getAmount());
        variables.put("message", request.getMessage());

        log.info("TEST 1");
        // Запуск процесса
        try {


            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                    "payment-process", variables);

            String processInstanceId = processInstance.getId();

            log.info("TEST 2");
            // Ждем результат через Execution Listener
            Map<String, Object> resultVariables = paymentListener
                    .waitForResult(processInstanceId)
                    .get(30, TimeUnit.SECONDS);

            log.info("TEST 3");
            PaymentResponseDTO response = buildResponse((String) variables.get("transactionId"), resultVariables);

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
        // Подготовка переменных
        Map<String, Object> variables = new HashMap<>();
        variables.putAll(securityService.getAuthVariables());
        variables.put("transactionId", transactionId);

        log.info("TEST 1");
        // Запуск процесса
        try {


            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                    "payment-status-process", variables);

            String processInstanceId = processInstance.getId();

            log.info("TEST 2");
            // Ждем результат через Execution Listener
            Map<String, Object> resultVariables = paymentStatusListener
                    .waitForResult(processInstanceId)
                    .get(60, TimeUnit.SECONDS);

            log.info("TEST 3");
            PaymentResponseDTO response = buildResponse(transactionId, resultVariables);

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


    private PaymentResponseDTO buildResponse(String transactionId, Map<String, Object> variables) {
        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setTransactionId(transactionId);
        response.setStatus((String) variables.get("status"));
        response.setSenderBillId((Long) variables.get("senderBillId"));
        response.setReceiverBillId((Long) variables.get("receiverBillId"));
        response.setAmount((BigDecimal) variables.get("amount"));
        response.setCommission((BigDecimal) variables.get("commission"));
        response.setMessage((String) variables.get("message"));
        response.setCreatedAt((LocalDateTime) variables.get("createdAt"));
        response.setCompletedAt((LocalDateTime) variables.get("completedAt"));
        return response;
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