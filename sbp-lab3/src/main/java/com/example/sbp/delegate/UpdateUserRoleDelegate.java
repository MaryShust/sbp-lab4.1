package com.example.sbp.delegate;

import com.example.sbp.dto.UserResponseDTO;
import com.example.sbp.exception.RoleNotFoundException;
import com.example.sbp.repository.RoleRepository;
import com.example.sbp.security.XmlUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateUserRoleDelegate implements JavaDelegate {

    private final RoleRepository roleRepository;
    private final XmlUserDetailsService userDetailsService;

    @Override
    public void execute(DelegateExecution execution) {
        String username = (String) execution.getVariable("targetUsername");
        String role = (String) execution.getVariable("role");

        String upperRole = role.toUpperCase();
        if (roleRepository.findByName(upperRole).isEmpty()) {
            execution.setVariable("bpmnError", "ROLE_NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", "Роль не найдена: " + role);
            execution.setVariable("roleUpdated", false);
            return;
        }

        try {
            userDetailsService.updateUserRole(username, upperRole);
            UserResponseDTO response = new UserResponseDTO(username, upperRole);

            execution.setVariable("username", response.getUsername());
            execution.setVariable("role", response.getRole());
            execution.setVariable("roleUpdated", true);
        } catch (RoleNotFoundException e) {
            execution.setVariable("bpmnError", "USER_NOT_FOUND");
            execution.setVariable("bpmnErrorMessage", "Пользователь не найден: " + username);
            execution.setVariable("roleUpdated", false);
        }
    }
}