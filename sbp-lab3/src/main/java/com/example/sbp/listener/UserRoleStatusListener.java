package com.example.sbp.listener;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class UserRoleStatusListener implements ExecutionListener {

    private final Map<String, CompletableFuture<Map<String, Object>>> futures = new ConcurrentHashMap<>();

    public CompletableFuture<Map<String, Object>> waitForResult(String processInstanceId) {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        futures.put(processInstanceId, future);
        return future;
    }

    @Override
    public void notify(DelegateExecution execution) throws Exception {
        String processInstanceId = execution.getProcessInstanceId();

        CompletableFuture<Map<String, Object>> future = futures.remove(processInstanceId);
        if (future != null) {
            Map<String, Object> variables = execution.getVariables();
            if (variables.containsKey("bpmnError")) {
                String errorCode = (String) variables.get("bpmnError");
                String errorMessage = (String) variables.get("bpmnErrorMessage");
                future.completeExceptionally(new BpmnError(errorCode, errorMessage));
            } else {
                future.complete(variables);
            }
        }
    }
}