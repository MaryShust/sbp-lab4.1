package com.example.sbp.delegate;

import com.example.sbp.security.XmlUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GetUserRoleDelegate implements JavaDelegate {

    private final XmlUserDetailsService userDetailsService;

    @Override
    public void execute(DelegateExecution execution) {
        String userName = (String) execution.getVariable("userName");

        execution.setVariable("username", userName);
        execution.setVariable("role", userDetailsService.getUser(userName).getRole());
        execution.setVariable("roleFound", true);
    }
}