package com.example.sbp.delegate;

import com.example.sbp.entity.BillEntity;
import com.example.sbp.entity.BankAccountEntity;
import com.example.sbp.repository.BankAccountRepository;
import com.example.sbp.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReplenishBillDelegate implements JavaDelegate {

    private final BillRepository billRepository;
    private final BankAccountRepository accountRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long accountId = (Long) execution.getVariable("accountId");
        Long billId = (Long) execution.getVariable("billId");
        BigDecimal amount = (BigDecimal) execution.getVariable("amount");

        log.info("TESTT ReplenishBillDelegate start: accountId={}, billId={}, amount={}", accountId, billId, amount);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("TESTT ReplenishBillDelegate BAD_REQUEST: amount={}", amount);
            execution.setVariable("hasError", true);
            execution.setVariable("bpmnError", "BAD_REQUEST");
            execution.setVariable("bpmnErrorMessage", "Сумма пополнения должна быть положительной");
            return;
        }

        BillEntity bill = billRepository.findById(billId)
                .orElse(null);
        if (bill == null) {
            log.info("TESTT ReplenishBillDelegate BILL_NOT_FOUND: billId={}", billId);
            execution.setVariable("hasError", true);
            execution.setVariable("bpmnError", "BILL_NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", "Счет не найден по id: " + billId);
            return;
        }

        BankAccountEntity account = accountRepository.findById(bill.getAccountId())
                .orElse(null);
        if (account == null) {
            log.info("TESTT ReplenishBillDelegate ACCOUNT_NOT_FOUND: accountId={}", bill.getAccountId());
            execution.setVariable("hasError", true);
            execution.setVariable("bpmnError", "ACCOUNT_NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", "Аккаунт не найден для счета id: " + billId);
            return;
        }

        if (!account.getId().equals(accountId)) {
            log.info("TESTT ReplenishBillDelegate BILL_NOT_BELONG_ACCOUNT: expected={}, actual={}", accountId, account.getId());
            execution.setVariable("hasError", true);
            execution.setVariable("bpmnError", "BILL_NOT_BELONG_ACCOUNT");
            execution.setVariable("bpmnErrorMessage", "Счет не принадлежит аккаунту");
            return;
        }

        if (!account.getIsActive()) {
            log.info("TESTT ReplenishBillDelegate INACTIVE: accountId={}", account.getId());
            execution.setVariable("hasError", true);
            execution.setVariable("bpmnError", "INACTIVE");
            execution.setVariable("bpmnErrorMessage", "Аккаунт отправителя не активен");
            return;
        }

        if (!bill.getIsActive()) {
            bill.setIsActive(true);
        }

        BigDecimal newBalance = bill.getBalance().add(amount);
        bill.setBalance(newBalance);
        billRepository.save(bill);

        log.info("TESTT ReplenishBillDelegate success: billId={}, newBalance={}", bill.getId(), newBalance);

        execution.setVariable("billId", bill.getId());
        execution.setVariable("accountId", bill.getAccountId());
        execution.setVariable("balance", bill.getBalance());
        execution.setVariable("isActive", bill.getIsActive());
        execution.setVariable("createdAt", bill.getCreatedAt());
        execution.setVariable("updatedAt", bill.getUpdatedAt());
        execution.setVariable("hasError", false);
        execution.removeVariable("bpmnError");
        execution.removeVariable("bpmnErrorMessage");
    }
}