package com.example.sbp.delegate;

import com.example.sbp.dto.BillResponseDTO;
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
public class CreateBillDelegate implements JavaDelegate {

    private final BillRepository billRepository;
    private final BankAccountRepository accountRepository;

    @Override
    public void execute(DelegateExecution execution) {
        Long accountId = (Long) execution.getVariable("targetAccountId");

        BankAccountEntity account = accountRepository.findById(accountId).orElse(null);
        if (account == null) {
            execution.setVariable("bpmnError", "ACCOUNT_NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", "Аккаунт не найден по id: " + accountId);
            execution.setVariable("billCreated", false);
            return;
        }

        BillEntity billEntity = BillEntity.builder()
                .accountId(accountId)
                .balance(BigDecimal.ZERO)
                .isActive(true)
                .build();
        billEntity = billRepository.save(billEntity);

        account.getAllBillIds().add(billEntity.getId());
        accountRepository.save(account);

        BillResponseDTO response = mapToResponseDTO(billEntity);

        execution.setVariable("id", response.getId());
        execution.setVariable("accountId", response.getAccountId());
        execution.setVariable("balance", response.getBalance());
        execution.setVariable("isActive", response.getIsActive());
        execution.setVariable("createdAt", response.getCreatedAt());
        execution.setVariable("updatedAt", response.getUpdatedAt());
        execution.setVariable("billCreated", true);
    }

    private BillResponseDTO mapToResponseDTO(BillEntity billEntity) {
        BillResponseDTO dto = new BillResponseDTO();
        dto.setId(billEntity.getId());
        dto.setAccountId(billEntity.getAccountId());
        dto.setBalance(billEntity.getBalance());
        dto.setIsActive(billEntity.getIsActive());
        dto.setCreatedAt(billEntity.getCreatedAt());
        dto.setUpdatedAt(billEntity.getUpdatedAt());
        return dto;
    }
}