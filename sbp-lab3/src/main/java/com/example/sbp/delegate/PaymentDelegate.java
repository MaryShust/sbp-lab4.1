package com.example.sbp.delegate;

import com.example.sbp.entity.BillEntity;
import com.example.sbp.entity.BankAccountEntity;
import com.example.sbp.entity.SbpTransactionEntity;
import com.example.sbp.exception.BankAccountNotFoundException;
import com.example.sbp.exception.BillInactiveException;
import com.example.sbp.exception.BillNotFoundException;
import com.example.sbp.kafka.producer.TransactionEventProducer;
import com.example.sbp.repository.BankAccountRepository;
import com.example.sbp.repository.BillRepository;
import com.example.sbp.repository.SbpTransactionRepository;
import com.example.sbp.security.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentDelegate implements JavaDelegate {

    private final SecurityService securityService;
    private final BillRepository billRepository;
    private final BankAccountRepository accountRepository;
    private final SbpTransactionRepository transactionRepository;
    private final TransactionEventProducer transactionEventProducer;


    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("TEST Checking if accounts are active");

        BigDecimal amount = (BigDecimal) execution.getVariable("amount");
//        BigDecimal amount = new BigDecimal(amountStr);
        String message = (String) execution.getVariable("message");
        String senderBankBic = (String) execution.getVariable("senderBankBic");
        String receiverBankBic = (String) execution.getVariable("receiverBankBic");
        Long senderBillId = (Long) execution.getVariable("senderBillId");
        Long receiverAccountId = (Long) execution.getVariable("receiverAccountId");
        BigDecimal commission = (BigDecimal) execution.getVariable("commission");
        String receiverIdentifier = (String) execution.getVariable("receiverIdentifier");
//        BigDecimal commission = new BigDecimal(commissionStr);


        try {
            log.info("TEST 1");
            BillEntity senderBillEntity = billRepository.findById(senderBillId)
                    .orElseThrow(() -> new BillNotFoundException("Не найден счет отправителя по id: " + senderBillId));
            log.info("TEST 2");
            BillEntity receiverBillEntity = findReceiverBill(receiverIdentifier);
            log.info("TEST 3");

            SbpTransactionEntity transaction = createTransaction(
                    senderBillEntity,
                    receiverBillEntity,
                    senderBankBic,
                    receiverBankBic,
                    amount,
                    message,
                    commission
            );
            log.info("TEST 4");
            updateBalances(senderBillEntity, receiverBillEntity, amount, commission);
            log.info("TEST 5");
            // Обновить статус транзакции
            transaction.setStatus(SbpTransactionEntity.TransactionStatus.SUCCESS);
            transaction.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            log.info("TEST 6");
            transactionEventProducer.sendTransactionEvent(transaction);
            log.info("TEST 7");
            execution.setVariable("transactionId", transaction.getTransactionId());
            execution.setVariable("status", transaction.getStatus().toString());
            execution.setVariable("senderBillId", transaction.getSenderBillId());
            execution.setVariable("receiverBillId", transaction.getReceiverBillId());
            execution.setVariable("amount", transaction.getAmount());
            execution.setVariable("commission", transaction.getCommission());
            execution.setVariable("message", transaction.getMessage());
            execution.setVariable("createdAt", transaction.getCreatedAt());
            execution.setVariable("completedAt", transaction.getCompletedAt());
            log.info("TEST 8");
        } catch (BillNotFoundException | BankAccountNotFoundException notFoundEx) {
            log.info("TEST 9");
            execution.setVariable("isPaymentCorrect", false);
            execution.setVariable("bpmnError", "NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", notFoundEx.getMessage());
            return;
        } catch (Exception ex) {
            log.info("TEST 10");
            log.info("TEST ERROR" + ex.getMessage());
            execution.setVariable("isPaymentCorrect", false);
            execution.setVariable("bpmnError", "NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", ex.getMessage());
            return;
        }
        log.info("TEST 11");
        execution.setVariable("isPaymentCorrect", true);
    }

    private SbpTransactionEntity createTransaction(
            BillEntity senderBillEntity,
            BillEntity receiverBillEntity,
            String senderBankBic,
            String receiverBankBic,
            BigDecimal amount,
            String message,
            BigDecimal commission
    ) {
        SbpTransactionEntity transaction = SbpTransactionEntity.builder()
                .senderBillId(senderBillEntity.getId())
                .senderBankBic(senderBankBic)
                .receiverBillId(receiverBillEntity.getId())
                .receiverBankBic(receiverBankBic)
                .amount(amount)
                .commission(commission)
                .status(SbpTransactionEntity.TransactionStatus.PENDING)
                .message(message)
                .build();
        return transactionRepository.save(transaction);
    }

    private void updateBalances(
            BillEntity senderBillEntity,
            BillEntity receiverBillEntity,
            BigDecimal amount,
            BigDecimal commission
    ) {
        // Списать с отправителя
        senderBillEntity.setBalance(senderBillEntity.getBalance()
                .subtract(amount)
                .subtract(commission));
        billRepository.save(senderBillEntity);

        // Зачислить получателю
        receiverBillEntity.setBalance(receiverBillEntity.getBalance().add(amount));
        billRepository.save(receiverBillEntity);
    }

    private BillEntity findReceiverBill(String identifier) {
        // Сначала пробуем найти как ID счета
        Optional<BillEntity> billById = tryFindBillById(identifier);

        // Если нашли по ID - возвращаем
        return billById.orElseGet(() -> findDefaultBillByPhone(identifier));
    }

    private Optional<BillEntity> tryFindBillById(String identifier) {
        try {
            Long billId = Long.parseLong(identifier);
            return billRepository.findById(billId);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private BillEntity findDefaultBillByPhone(String identifier) {
        BankAccountEntity account = accountRepository.findByPhoneNumber(identifier)
                .orElseThrow(() -> new BankAccountNotFoundException(
                        "Аккаунт не найден по телефону: " + identifier));

        return billRepository.findById(account.getDefaultBillId())
                .orElseThrow(() -> new BillNotFoundException(
                        "Дефолтный счет не найден для аккаунта с телефоном: " + identifier));
    }
}