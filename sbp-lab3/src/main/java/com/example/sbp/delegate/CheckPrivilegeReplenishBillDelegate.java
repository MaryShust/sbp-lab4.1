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
public class CheckPrivilegeReplenishBillDelegate implements JavaDelegate {

    private final SecurityService securityService;

    @Override
    public void execute(DelegateExecution execution) {
        Long billId = (Long) execution.getVariable("billId");
        String userName = (String) execution.getVariable("userName");
        Long accountId = (Long) execution.getVariable("accountId");
        Set<Privilege> privileges = (Set<Privilege>) execution.getVariable("privileges");

        log.info("TESTT CheckPrivilegeReplenishBillDelegate start: userName={}, accountId={}, billId={}, privileges={}",
                userName, accountId, billId, privileges);

        if (userName != null &&
                privileges.contains(Privilege.BILL_REPLENISH) &&
                securityService.isBillOwnedByCurrentUser(billId, userName, accountId)
        ) {
            log.info("TESTT CheckPrivilegeReplenishBillDelegate GRANTED");
            execution.setVariable("hasPrivilege", true);
            return;
        }

        log.info("TESTT CheckPrivilegeReplenishBillDelegate DENIED");
        execution.setVariable("hasError", true);
        execution.setVariable("hasPrivilege", false);
        execution.setVariable("bpmnError", "ACCESS_DENIED");
        execution.setVariable("bpmnErrorMessage", "Недостаточно прав для пополнения счета");
    }
}