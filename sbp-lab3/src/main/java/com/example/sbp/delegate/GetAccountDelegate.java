package com.example.sbp.delegate;

import com.example.sbp.dto.BankAccountResponseDTO;
import com.example.sbp.exception.BankAccountNotFoundException;
import com.example.sbp.service.BankAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GetAccountDelegate implements JavaDelegate {

    private final BankAccountService bankAccountService;

    @Override
    public void execute(DelegateExecution execution) {
        Long id = (Long) execution.getVariable("id");

        try {
            BankAccountResponseDTO response = bankAccountService.getAccountById(id);

            execution.setVariable("id", response.getId());
            execution.setVariable("phoneNumber", response.getPhoneNumber());
            execution.setVariable("ownerName", response.getOwnerName());
            execution.setVariable("bankBic", response.getBankBic());
            execution.setVariable("isActive", response.getIsActive());
            execution.setVariable("createdAt", response.getCreatedAt());
            execution.setVariable("updatedAt", response.getUpdatedAt());
            execution.setVariable("defaultBillId", response.getDefaultBillId());
            execution.setVariable("allBillIds", response.getAllBillIds());
            execution.setVariable("accountFound", true);
        } catch (BankAccountNotFoundException e) {
            execution.setVariable("bpmnError", "ACCOUNT_NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", "Аккаунт не найден: " + id);
            execution.setVariable("accountFound", false);
        }
    }
}