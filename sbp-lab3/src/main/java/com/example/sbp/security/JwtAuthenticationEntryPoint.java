package com.example.sbp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        log.debug("JwtAuthenticationEntryPoint commence");

        Boolean jwtExpired = (Boolean) request.getAttribute("jwt_expired");

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body;
        if (Boolean.TRUE.equals(jwtExpired)) {
            response.setStatus(498);
            body = Map.of(
                    "error", "Token expired",
                    "message", "Token is outdated"
            );
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            body = Map.of(
                    "error", "Unauthorized",
                    "message", "Authentication required"
            );
        }

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
