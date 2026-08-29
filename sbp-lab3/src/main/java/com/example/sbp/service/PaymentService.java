package com.example.sbp.service;

import com.example.sbp.dto.PaymentRequestDTO;
import com.example.sbp.dto.PaymentResponseDTO;
import com.example.sbp.entity.BankAccountEntity;
import com.example.sbp.entity.BillEntity;
import com.example.sbp.entity.SbpTransactionEntity;
import com.example.sbp.kafka.producer.TransactionEventProducer;
import com.example.sbp.repository.BankAccountRepository;
import com.example.sbp.repository.BillRepository;
import com.example.sbp.repository.SbpTransactionRepository;
import com.example.sbp.exception.*;
import com.example.sbp.security.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final SecurityService securityService;
    private final BillRepository billRepository;
    private final BankAccountRepository accountRepository;
    private final SbpTransactionRepository transactionRepository;
    private final TransactionEventProducer transactionEventProducer;

    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.005"); // 0.5%
    private static final BigDecimal MIN_COMMISSION = new BigDecimal("10");
    private static final BigDecimal MAX_COMMISSION = new BigDecimal("1000");

    @Transactional(transactionManager = "transactionManager")
    public PaymentResponseDTO processPayment(PaymentRequestDTO request) {
        securityService.checkPrivilegeCreatePayment(request.getSenderBillId());

        if (request.getMessage().trim().length() > 100) {
            throw new MessageFormatException("Сообщение не может быть длиннее 100 символов");
        }

        BillEntity senderBillEntity = billRepository.findById(request.getSenderBillId())
                .orElseThrow(() -> new BillNotFoundException("Не найден счет отправителя по id: " + request.getSenderBillId()));

        BankAccountEntity senderAccount = accountRepository.findById(senderBillEntity.getAccountId())
                .orElseThrow(() -> new BankAccountNotFoundException("Аккаунт не найден с id: " + senderBillEntity.getAccountId()));

        if (!senderAccount.getIsActive()) {
            throw new BillInactiveException("Аккаунт отправителя не активен");
        }
        if (!senderBillEntity.getIsActive()) {
            throw new BillInactiveException("Счет отправителя не активен");
        }

        BillEntity receiverBillEntity = findReceiverBill(request.getReceiverIdentifier());

        // Проверять аккаунт не нужно, так как если он заблочен или на него наложен арест, то деньжата уйдут приставам
        if (!receiverBillEntity.getIsActive()) {
            throw new BillInactiveException("Счет получателя не активен");
        }

        BankAccountEntity receiverAccount = accountRepository.findById(receiverBillEntity.getAccountId())
                .orElseThrow(() -> new BankAccountNotFoundException("Аккаунт не найден с id: " + senderBillEntity.getAccountId()));

        BigDecimal commission = BigDecimal.ZERO;
        if (!senderAccount.getId().equals(receiverAccount.getId())) {
            commission = calculateCommission(request.getAmount());
        }

        // Проверить достаточность средств
        BigDecimal totalAmount = request.getAmount().add(commission);
        if (senderBillEntity.getBalance().compareTo(totalAmount) < 0) {
            throw new InsufficientFundsException("Недостаточно средств на счете отправителя");
        }

        SbpTransactionEntity transaction = createTransaction(
                senderBillEntity,
                receiverBillEntity,
                senderAccount.getBankBic(),
                receiverAccount.getBankBic(),
                request,
                commission
        );

        updateBalances(senderBillEntity, receiverBillEntity, request.getAmount(), commission);

        // Обновить статус транзакции
        transaction.setStatus(SbpTransactionEntity.TransactionStatus.SUCCESS);
        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        transactionEventProducer.sendTransactionEvent(transaction);

        return convertToResponseDTO(transaction);
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

    private BigDecimal calculateCommission(BigDecimal amount) {
        BigDecimal commission = amount.multiply(COMMISSION_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        // Проверка минимальной и максимальной комиссии
        if (commission.compareTo(MIN_COMMISSION) < 0) {
            return MIN_COMMISSION;
        }
        if (commission.compareTo(MAX_COMMISSION) > 0) {
            return MAX_COMMISSION;
        }
        return commission;
    }

    private SbpTransactionEntity createTransaction(
            BillEntity senderBillEntity,
            BillEntity receiverBillEntity,
            String senderBankBic,
            String receiverBankBic,
            PaymentRequestDTO request,
            BigDecimal commission
    ) {
        SbpTransactionEntity transaction = SbpTransactionEntity.builder()
                .senderBillId(senderBillEntity.getId())
                .senderBankBic(senderBankBic)
                .receiverBillId(receiverBillEntity.getId())
                .receiverBankBic(receiverBankBic)
                .amount(request.getAmount())
                .commission(commission)
                .status(SbpTransactionEntity.TransactionStatus.PENDING)
                .message(request.getMessage())
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

    private PaymentResponseDTO convertToResponseDTO(SbpTransactionEntity transaction) {
        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setTransactionId(transaction.getTransactionId());
        response.setStatus(transaction.getStatus().toString());
        response.setSenderBillId(transaction.getSenderBillId());
        response.setReceiverBillId(transaction.getReceiverBillId());
        response.setAmount(transaction.getAmount());
        response.setCommission(transaction.getCommission());
        response.setMessage(transaction.getMessage());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setCompletedAt(transaction.getCompletedAt());

        return response;
    }

    public PaymentResponseDTO getTransactionStatus(String transactionId) {
        SbpTransactionEntity transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Транзакция не найдена по id"));

        return convertToResponseDTO(transaction);
    }
}