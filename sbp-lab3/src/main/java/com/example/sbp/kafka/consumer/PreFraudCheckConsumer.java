package com.example.sbp.kafka.consumer;

import com.example.sbp.entity.BillEntity;
import com.example.sbp.entity.PreSuspicionEntity;
import com.example.sbp.kafka.dto.FraudTransactionDTO;
import com.example.sbp.kafka.dto.RiskLevel;
import com.example.sbp.repository.BillRepository;
import com.example.sbp.repository.PreSuspicionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreFraudCheckConsumer {

    private final PreSuspicionRepository preSuspicionRepository;
    private final BillRepository billRepository;

    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("200000");
    private static final BigDecimal CRITICAL_AMOUNT_THRESHOLD = new BigDecimal("500000");

    @KafkaListener(
            topics = "sbp-fraud-check",
            containerFactory = "fraudCheckKafkaListenerContainerFactory"
    )
    public void handleFraudCheckRequest(FraudTransactionDTO request) {
        log.info("Обработка запроса на проверку на мошенничество по транзакции: {}", request.getTransactionId());

        Long receiverAccountId = billRepository.findById(request.getReceiverBillId())
                .map(BillEntity::getAccountId)
                .orElse(null);

        Long senderAccountId = billRepository.findById(request.getSenderBillId())
                .map(BillEntity::getAccountId)
                .orElse(null);

        if (receiverAccountId == null) {
            return;
        }

        RiskLevel riskLevel = determineRiskLevel(request);

        PreSuspicionEntity preSuspicion = PreSuspicionEntity.builder()
                .transactionId(request.getTransactionId())
                .receiverAccountId(receiverAccountId)
                .receiverBankBic(request.getReceiverBankBic())
                .senderAccountId(senderAccountId)
                .senderBillId(request.getSenderBillId())
                .senderBankBic(request.getSenderBankBic())
                .amount(request.getAmount())
                .eventTime(request.getCreatedAt())
                .riskLevel(riskLevel)
                .build();

        preSuspicionRepository.save(preSuspicion);

        log.info("Предварительная проверка на предмет мошенничества завершена: transactionId={}, riskLevel={}",
                request.getTransactionId(), riskLevel);

    }

    /*
    Если перевод самому себе из одного банка, то LOW
    Если перевод в рамках одного банка в странное время (до 10:00 и после 20:00) и сумма превышает 200т, но меньше 500т, то HIGH
    Если перевод в рамках одного банка в странное время (до 10:00 и после 20:00) и сумма превышает 500т, то CRITICAL
    Иначе MEDIUM
     */
    private RiskLevel determineRiskLevel(FraudTransactionDTO fraudTransaction) {
        LocalTime time = fraudTransaction.getCreatedAt().toLocalTime();

        if (fraudTransaction.getSenderBankBic().equals(fraudTransaction.getReceiverBankBic()) &&
                fraudTransaction.getSenderBillId().equals(fraudTransaction.getReceiverBillId())) {
            return RiskLevel.LOW;
        } else if (
                fraudTransaction.getSenderBankBic().equals(fraudTransaction.getReceiverBankBic()) &&
                        (time.isBefore(LocalTime.of(10, 0)) || time.isAfter(LocalTime.of(14, 0)))
        ) {
            if (fraudTransaction.getAmount().compareTo(CRITICAL_AMOUNT_THRESHOLD) > 0) {
                return RiskLevel.CRITICAL;
            } else if (fraudTransaction.getAmount().compareTo(HIGH_AMOUNT_THRESHOLD) > 0) {
                return RiskLevel.HIGH;
            } else {
                return RiskLevel.MEDIUM;
            }
        } else {
            return RiskLevel.MEDIUM;
        }
    }
}