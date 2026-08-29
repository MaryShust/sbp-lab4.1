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
public class CheckPrivilegeReadPaymentStatusDelegate implements JavaDelegate {

    private final SecurityService securityService;

    @Override
    public void execute(DelegateExecution execution) {
        String transactionId = (String) execution.getVariable("transactionId");
        String userName = (String) execution.getVariable("userName");
        Long accountId = (Long) execution.getVariable("accountId");

        Set<Privilege> privileges = (Set<Privilege>) execution.getVariable("privileges");

        if (userName != null && privileges.contains(Privilege.PAYMENT_SUPER_READ_STATUS)) {
            execution.setVariable("hasPrivilege", true);
            return;
        }

        if (userName != null &&
                privileges.contains(Privilege.PAYMENT_READ_STATUS) &&
                securityService.isTransactionRelatedToCurrentUser(transactionId, userName, accountId)
        ) {
            execution.setVariable("hasPrivilege", true);
            return;
        }

        execution.setVariable("hasPrivilege", false);
    }
}