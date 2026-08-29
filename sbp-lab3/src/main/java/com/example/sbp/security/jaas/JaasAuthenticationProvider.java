package com.example.sbp.security.jaas;

import com.example.sbp.security.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

@Slf4j
@Component
public class JaasAuthenticationProvider implements AuthenticationProvider {

    static {
        JaasConfiguration.register();
        log.debug("JAAS конфигурация зарегистрирована");
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials() != null ? authentication.getCredentials().toString() : "";

        try {
            CallbackHandler callbackHandler = callbacks -> {
                for (Callback callback : callbacks) {
                    if (callback instanceof NameCallback) {
                        ((NameCallback) callback).setName(username);
                    } else if (callback instanceof PasswordCallback) {
                        ((PasswordCallback) callback).setPassword(password.toCharArray());
                    } else {
                        throw new UnsupportedCallbackException(callback);
                    }
                }
            };

            LoginContext loginContext = new LoginContext(JaasConfiguration.APPLICATION_NAME, callbackHandler);
            loginContext.login();

            Subject subject = loginContext.getSubject();

            CustomUserDetails userDetails = subject.getPublicCredentials().stream()
                    .filter(CustomUserDetails.class::isInstance)
                    .map(CustomUserDetails.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new LoginException("UserDetails не найдены"));

            return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        } catch (LoginException e) {
            log.debug("Вход по JAAS зафейлился с {}: {}", username, e.getMessage());
            throw new BadCredentialsException(e.getMessage(), e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
