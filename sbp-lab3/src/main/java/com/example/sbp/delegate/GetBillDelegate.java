package com.example.sbp.delegate;

import com.example.sbp.dto.BillResponseDTO;
import com.example.sbp.exception.BillNotFoundException;
import com.example.sbp.service.BillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GetBillDelegate implements JavaDelegate {

    private final BillService billService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long billId = (Long) execution.getVariable("billId");

        try {
            BillResponseDTO response = billService.getBillById(billId);

            // Установка переменных для результата
            execution.setVariable("billId", response.getId());
            execution.setVariable("accountId", response.getAccountId());
            execution.setVariable("balance", response.getBalance());
            execution.setVariable("isActive", response.getIsActive());
            execution.setVariable("createdAt", response.getCreatedAt());
            execution.setVariable("updatedAt", response.getUpdatedAt());
            execution.setVariable("success", true);
        } catch (BillNotFoundException e) {
            execution.setVariable("error", e.getMessage());
            execution.setVariable("success", false);
        }
    }
}