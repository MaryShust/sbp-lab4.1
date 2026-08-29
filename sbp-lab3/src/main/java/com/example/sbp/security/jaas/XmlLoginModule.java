package com.example.sbp.security.jaas;

import com.example.sbp.security.CustomUserDetails;
import com.example.sbp.security.XmlUserDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.util.*;

@Slf4j
public class XmlLoginModule implements LoginModule {
    private Subject subject;
    private CallbackHandler callbackHandler;
    private String username;
    private CustomUserDetails userDetails;
    private boolean succeeded = false;
    private boolean commitSucceeded = false;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
    }

    @Override
    public boolean login() throws LoginException {
        if (callbackHandler == null) throw new LoginException("Нет CallbackHandler");

        NameCallback nameCallback = new NameCallback("Username: ");
        PasswordCallback passwordCallback = new PasswordCallback("Password: ", false);

        try {
            callbackHandler.handle(new Callback[]{nameCallback, passwordCallback});
        } catch (Exception e) {
            throw new LoginException("Ошибка получения данных: " + e.getMessage());
        }

        username = nameCallback.getName();
        String password = new String(passwordCallback.getPassword());

        try {
            XmlUserDetailsService userDetailsService = SpringApplicationContextHolder.getBean(XmlUserDetailsService.class);
            PasswordEncoder passwordEncoder = SpringApplicationContextHolder.getBean(PasswordEncoder.class);

            if (!userDetailsService.userExists(username)) {
                throw new LoginException("Пользователь не найден");
            }

            UserDetails details = userDetailsService.loadUserByUsername(username);
            if (details instanceof CustomUserDetails) {
                userDetails = (CustomUserDetails) details;
            } else {
                throw new LoginException("Неверный тип UserDetails");
            }

            if (!verifyPassword(password, userDetails.getPassword(), passwordEncoder)) {
                throw new LoginException("Неверный пароль");
            }

            if (!userDetails.isEnabled()) {
                throw new LoginException("Аккаунт отключен");
            }

            succeeded = true;
            log.debug("Пользователь успешно вошел в систему: {}", username);
            return true;
        } catch (LoginException e) {
            throw e;
        } catch (Exception e) {
            log.debug("Вход в систему для пользователя не удался: {}", username, e);
            succeeded = false;
            throw new LoginException("Ошибка: " + e.getMessage());
        }
    }

    @Override
    public boolean commit() throws LoginException {
        if (!succeeded || userDetails == null) return false;
        subject.getPublicCredentials().add(userDetails);
        commitSucceeded = true;
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        if (!succeeded) return false;
        if (!commitSucceeded) {
            succeeded = false;
            username = null;
            userDetails = null;
        } else {
            logout();
        }
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        subject.getPrincipals().clear();
        subject.getPublicCredentials().clear();
        succeeded = false;
        commitSucceeded = false;
        username = null;
        userDetails = null;
        return true;
    }

    private boolean verifyPassword(String rawPassword, String encodedPassword, PasswordEncoder encoder) {
        String clean = encodedPassword.startsWith("{bcrypt}") ? encodedPassword.substring(8) : encodedPassword;
        return encoder.matches(rawPassword, clean);
    }
}
