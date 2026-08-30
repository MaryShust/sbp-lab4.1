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
public class CheckPrivilegeReadBillDelegate implements JavaDelegate {

    private final SecurityService securityService;

    @Override
    public void execute(DelegateExecution execution) {
        Long billId = (Long) execution.getVariable("billId");
        String userName = (String) execution.getVariable("userName");
        Long accountId = (Long) execution.getVariable("accountId");
        Set<Privilege> privileges = (Set<Privilege>) execution.getVariable("privileges");

        // Проверка супер-привилегии (MANAGER)
        if (userName != null && privileges.contains(Privilege.BILL_SUPER_READ)) {
            execution.setVariable("hasPrivilege", true);
            return;
        }

        // Проверка обычной привилегии
        if (userName != null &&
                privileges.contains(Privilege.BILL_READ) &&
                privileges.contains(Privilege.BILL_READ_DEFAULT) &&
                securityService.isBillOwnedByCurrentUser(billId, userName, accountId)
        ) {
            execution.setVariable("hasPrivilege", true);
            return;
        }

        execution.setVariable("hasPrivilege", false);
    }
}