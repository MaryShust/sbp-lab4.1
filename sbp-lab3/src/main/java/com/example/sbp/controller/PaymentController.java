package com.example.sbp.controller;

import com.example.sbp.dto.PaymentRequestDTO;
import com.example.sbp.dto.PaymentResponseDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Переводы по СБП")
public class PaymentController {

    private final PaymentService paymentService;

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
    public ResponseEntity<?> getPaymentStatus(
            @Parameter(description = "ID транзакции", example = "SBP1234567890_123")
            @PathVariable String transactionId
    ) {
        PaymentResponseDTO response = paymentService.getTransactionStatus(transactionId);
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