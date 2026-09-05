package com.example.sbp.delegate;

import com.example.sbp.dto.BankAccountRequestDTO;
import com.example.sbp.dto.BankAccountResponseDTO;
import com.example.sbp.exception.BankAccountAlreadyExistsException;
import com.example.sbp.service.BankAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateAccountDelegate implements JavaDelegate {

    private final BankAccountService bankAccountService;

    @Override
    public void execute(DelegateExecution execution) {
        BankAccountRequestDTO request = new BankAccountRequestDTO();
        request.setPhoneNumber((String) execution.getVariable("phoneNumber"));
        request.setOwnerName((String) execution.getVariable("ownerName"));
        request.setBankBic((String) execution.getVariable("bankBic"));

        try {
            BankAccountResponseDTO response = bankAccountService.createAccount(request);

            execution.setVariable("id", response.getId());
            execution.setVariable("createdPhoneNumber", response.getPhoneNumber());
            execution.setVariable("defaultBillId", response.getDefaultBillId());
            execution.setVariable("accountCreated", true);
        } catch (BankAccountAlreadyExistsException e) {
            execution.setVariable("bpmnError", "ACCOUNT_ALREADY_EXISTS");
            execution.setVariable("bpmnErrorMessage", e.getMessage());
            execution.setVariable("accountCreated", false);
        }
    }
}