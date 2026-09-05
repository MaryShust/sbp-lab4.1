package com.example.sbp.delegate;

import com.example.sbp.entity.BankAccountEntity;
import com.example.sbp.entity.BillEntity;
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
public class ActivateDefaultBillDelegate implements JavaDelegate {

    private final BankAccountRepository accountRepository;
    private final BillRepository billRepository;

    @Override
    public void execute(DelegateExecution execution) {
        Long accountId = (Long) execution.getVariable("targetAccountId");
        BigDecimal startBalance = (BigDecimal) execution.getVariable("startBalance");

        BankAccountEntity account = accountRepository.findById(accountId).orElse(null);
        if (account == null) {
            execution.setVariable("bpmnError", "ACCOUNT_NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", "Аккаунт не найден с id: " + accountId);
            execution.setVariable("billActivated", false);
            return;
        }

        BillEntity defaultBill = billRepository.findById(account.getDefaultBillId()).orElse(null);
        if (defaultBill == null) {
            execution.setVariable("bpmnError", "BILL_NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", "Дефолтный счет не найден");
            execution.setVariable("billActivated", false);
            return;
        }

        if (startBalance == null || startBalance.compareTo(BigDecimal.ZERO) < 0) {
            execution.setVariable("bpmnError", "BAD_REQUEST");
            execution.setVariable("bpmnErrorMessage", "Начальный баланс должен быть неотрицательным");
            execution.setVariable("billActivated", false);
            return;
        }

        if (defaultBill.getBalance().compareTo(BigDecimal.ZERO) == 0) {
            defaultBill.setIsActive(true);
            defaultBill.setBalance(startBalance);
            billRepository.save(defaultBill);
        }

        execution.setVariable("billActivated", true);
    }
}