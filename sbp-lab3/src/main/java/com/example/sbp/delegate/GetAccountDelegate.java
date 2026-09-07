package com.example.sbp.delegate;

import com.example.sbp.entity.BankAccountEntity;
import com.example.sbp.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GetAccountDelegate implements JavaDelegate {

    private final BankAccountRepository bankAccountRepository;

    @Override
    public void execute(DelegateExecution execution) {
        Long id = (Long) execution.getVariable("id");
        BankAccountEntity account = bankAccountRepository.findById(id).orElse(null);

        if (account == null) {
            execution.setVariable("bpmnError", "ACCOUNT_NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", "Аккаунт не найден: " + id);
            execution.setVariable("accountFound", false);
            return;
        }

        execution.setVariable("id", account.getId());
        execution.setVariable("phoneNumber", account.getPhoneNumber());
        execution.setVariable("ownerName", account.getOwnerName());
        execution.setVariable("bankBic", account.getBankBic());
        execution.setVariable("isActive", account.getIsActive());
        execution.setVariable("createdAt", account.getCreatedAt());
        execution.setVariable("updatedAt", account.getUpdatedAt());
        execution.setVariable("defaultBillId", account.getDefaultBillId());
        ArrayList<Long> test = new ArrayList();

        List<Long> test2 = account.getAllBillIds();
        test.addAll(test2);

        execution.setVariable("allBillIds", test);
        execution.setVariable("accountFound", true);
    }
}