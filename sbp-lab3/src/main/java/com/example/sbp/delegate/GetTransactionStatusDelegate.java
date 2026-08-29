package com.example.sbp.delegate;

import com.example.sbp.dto.PaymentResponseDTO;
import com.example.sbp.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GetTransactionStatusDelegate implements JavaDelegate {

    private final PaymentService paymentService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String transactionId = (String) execution.getVariable("transactionId");
        log.info("TEST D: {}", transactionId);

        try {
            PaymentResponseDTO response = paymentService.getTransactionStatus(transactionId);
            execution.setVariable("status", response.getStatus());
            execution.setVariable("senderBillId", response.getSenderBillId());
            execution.setVariable("receiverBillId", response.getReceiverBillId());
            execution.setVariable("amount", response.getAmount());
            execution.setVariable("commission", response.getCommission());
            execution.setVariable("message", response.getMessage());
            execution.setVariable("createdAt", response.getCreatedAt());
            execution.setVariable("completedAt", response.getCompletedAt());
            execution.setVariable("success", true);
        } catch (Exception e) {
            log.error("TEST D2: {}", e.getMessage());
            execution.setVariable("error", e.getMessage());
            execution.setVariable("success", false);
            throw e;
        }
    }
}