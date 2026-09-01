package com.example.sbp.delegate;

import com.example.sbp.entity.BankAccountEntity;
import com.example.sbp.entity.BillEntity;
import com.example.sbp.exception.BankAccountNotFoundException;
import com.example.sbp.exception.BillInactiveException;
import com.example.sbp.exception.BillNotFoundException;
import com.example.sbp.repository.BankAccountRepository;
import com.example.sbp.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;

import javax.security.auth.login.AccountNotFoundException;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckSenderDelegate implements JavaDelegate {

    private final BillRepository billRepository;
    private final BankAccountRepository accountRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("TEST Getting sender bill");

        Long senderBillId = (Long) execution.getVariable("senderBillId");

        BigDecimal senderBalance;
        String senderBankBic;

        try {
            BillEntity senderBillEntity = billRepository.findById(senderBillId)
                    .orElseThrow(() -> new BillNotFoundException("Не найден счет отправителя по id: " + senderBillId));

            BankAccountEntity senderAccount = accountRepository.findById(senderBillEntity.getAccountId())
                    .orElseThrow(() -> new BankAccountNotFoundException("Аккаунт не найден с id: " + senderBillEntity.getAccountId()));

            senderBalance = senderBillEntity.getBalance();
            senderBankBic = senderAccount.getBankBic();

            if (!senderAccount.getIsActive()) {
                throw new BillInactiveException("Аккаунт отправителя не активен");
            }
            if (!senderBillEntity.getIsActive()) {
                throw new BillInactiveException("Счет отправителя не активен");
            }
        } catch (BillNotFoundException | BankAccountNotFoundException notFoundEx) {
            execution.setVariable("isSenderCorrect", false);
            execution.setVariable("bpmnError", "NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", notFoundEx.getMessage());
            return;
        } catch (BillInactiveException billEx) {
            execution.setVariable("isSenderCorrect", false);
            execution.setVariable("bpmnError", "INACTIVE");
            execution.setVariable("bpmnErrorMessage", billEx.getMessage());
            return;
        }

        execution.setVariable("isSenderCorrect", true);
        execution.setVariable("senderBalance", senderBalance);
        execution.setVariable("senderBankBic", senderBankBic);
    }
}