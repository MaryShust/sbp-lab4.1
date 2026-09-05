package com.example.sbp.delegate;

import com.example.sbp.security.Privilege;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
@Slf4j
public class CheckPrivilegeCreateBillDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        Long targetAccountId = (Long) execution.getVariable("targetAccountId");
        String userName = (String) execution.getVariable("userName");
        Long currentUserAccountId = (Long) execution.getVariable("accountId");
        Set<Privilege> privileges = (Set<Privilege>) execution.getVariable("privileges");

        if (userName != null &&
                privileges.contains(Privilege.BILL_CREATE) &&
                currentUserAccountId != null &&
                targetAccountId != null &&
                currentUserAccountId.equals(targetAccountId)
        ) {
            execution.setVariable("hasPrivilege", true);
            return;
        }

        execution.setVariable("hasPrivilege", false);
        execution.setVariable("bpmnError", "ACCESS_DENIED");
        execution.setVariable("bpmnErrorMessage", "Недостаточно прав для создания счета");
    }
}