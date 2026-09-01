package com.example.sbp.delegate;

import com.example.sbp.security.Privilege;
import com.example.sbp.security.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckPrivilegePaymentDelegate implements JavaDelegate {

    private final SecurityService securityService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("TEST Checking privileges for payment");

        Long senderBillId = (Long) execution.getVariable("senderBillId");
        String userName = (String) execution.getVariable("userName");
        Long accountId = (Long) execution.getVariable("accountId");
        Set<Privilege> privileges = (Set<Privilege>) execution.getVariable("privileges");

        if (userName != null &&
                privileges.contains(Privilege.PAYMENT_CREATE) && securityService.isBillOwnedByCurrentUser(
                        senderBillId, userName, accountId)
        ) {
            execution.setVariable("hasPrivilege", true);
            return;
        }

        execution.setVariable("hasPrivilege", false);
        execution.setVariable("bpmnError", "ACCESS_DENIED");
        execution.setVariable("bpmnErrorMessage", "Недостаточно прав для совершения платежа");

    }
}