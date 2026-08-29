package com.example.sbp.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AccessDeniedDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        execution.setVariable("error", "Не достаточно прав");
        execution.setVariable("success", false);
    }
}