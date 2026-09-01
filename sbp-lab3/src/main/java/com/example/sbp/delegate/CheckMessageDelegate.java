package com.example.sbp.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CheckMessageDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String message = (String) execution.getVariable("message");

        log.info("TEST Validating message = " + message.trim().length());

        if (message != null && message.trim().length() > 100) {
            execution.setVariable("isMessageCorrect", false);
            execution.setVariable("bpmnError", "BAD_REQUEST");
            execution.setVariable("bpmnErrorMessage", "Сообщение не может быть длиннее 100 символов");
            return;
        }

        execution.setVariable("isMessageCorrect", true);
    }
}