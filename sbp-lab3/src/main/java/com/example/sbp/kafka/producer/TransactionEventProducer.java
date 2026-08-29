package com.example.sbp.kafka.producer;

import com.example.sbp.entity.SbpTransactionEntity;
import com.example.sbp.kafka.dto.FraudTransactionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${sbp.kafka.topics.fraud-check:sbp-fraud-check}")
    private String fraudCheckTopic;

    public void sendTransactionEvent(SbpTransactionEntity transaction) {
        FraudTransactionDTO message = FraudTransactionDTO.builder()
                .transactionId(transaction.getTransactionId())
                .senderBillId(transaction.getSenderBillId())
                .receiverBillId(transaction.getReceiverBillId())
                .amount(transaction.getAmount())
                .senderBankBic(transaction.getSenderBankBic())
                .receiverBankBic(transaction.getReceiverBankBic())
                .createdAt(transaction.getCreatedAt())
                .build();

        kafkaTemplate.send(
                fraudCheckTopic,
                transaction.getTransactionId(),
                message
        );
    }
}