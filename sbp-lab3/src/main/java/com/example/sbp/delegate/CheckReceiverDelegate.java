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
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckReceiverDelegate implements JavaDelegate {

    private final BillRepository billRepository;
    private final BankAccountRepository accountRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("TEST Getting receiver bill");

        String receiverIdentifier = (String) execution.getVariable("receiverIdentifier");

        Long receiverAccountId;
        String receiverBankBic;
        try {
            BillEntity receiverBillEntity = findReceiverBill(receiverIdentifier);

            // Проверять аккаунт не нужно, так как если он заблочен или на него наложен арест, то деньжата уйдут приставам
            if (!receiverBillEntity.getIsActive()) {
                throw new BillInactiveException("Счет получателя не активен");
            }

            BankAccountEntity receiverAccount = accountRepository.findById(receiverBillEntity.getAccountId())
                    .orElseThrow(() -> new BankAccountNotFoundException("Аккаунт получателя не найден"));

            receiverAccountId = receiverAccount.getId();
            receiverBankBic = receiverAccount.getBankBic();

        } catch (BillNotFoundException | BankAccountNotFoundException notFoundEx) {
            execution.setVariable("isReceiverCorrect", false);
            execution.setVariable("bpmnError", "NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", notFoundEx.getMessage());
            return;
        } catch (BillInactiveException billEx) {
            execution.setVariable("isReceiverCorrect", false);
            execution.setVariable("bpmnError", "INACTIVE");
            execution.setVariable("bpmnErrorMessage", billEx.getMessage());
            return;
        }

        execution.setVariable("isReceiverCorrect", true);
        execution.setVariable("receiverAccountId", receiverAccountId);
        execution.setVariable("receiverBankBic", receiverBankBic);
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