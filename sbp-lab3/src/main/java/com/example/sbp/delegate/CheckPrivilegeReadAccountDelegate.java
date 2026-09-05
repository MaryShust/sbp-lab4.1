package com.example.sbp.delegate;

import com.example.sbp.security.Privilege;
import com.example.sbp.security.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckPrivilegeReadAccountDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        Long accountId = (Long) execution.getVariable("id");
        String userName = (String) execution.getVariable("userName");
        Long currentUserAccountId = (Long) execution.getVariable("accountId");
        Set<Privilege> privileges = (Set<Privilege>) execution.getVariable("privileges");

        if (userName != null && privileges.contains(Privilege.ACCOUNT_SUPER_READ)) {
            execution.setVariable("hasPrivilege", true);
            return;
        }

        if (userName != null &&
                privileges.contains(Privilege.ACCOUNT_READ) &&
                privileges.contains(Privilege.ACCOUNT_READ_BY_PHONE) &&
                currentUserAccountId != null &&
                Objects.equals(currentUserAccountId, accountId)
        ) {
            execution.setVariable("hasPrivilege", true);
            return;
        }

        execution.setVariable("hasPrivilege", false);
    }
}