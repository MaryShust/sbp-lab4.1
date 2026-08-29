package com.example.sbp.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret:defaultSecretKeyForDevelopmentPurposesOnly123456789}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    private final XmlUserDetailsService userDetailsService;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        if (userDetails == null) {
            throw new BadCredentialsException("ошибка генерации токена");
        }

        String role = userDetails.getRole();

        String privileges = userDetails.getPrivileges().stream()
                .map(Privilege::name)
                .collect(Collectors.joining(","));

        var builder = Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("role", role)
                .claim("privileges", privileges)
                .claim("tokenVersion", userDetails.getTokenVersion())
                .issuedAt(now)
                .expiration(expiryDate);

        if (userDetails.getAccountId() != null) {
            builder.claim("accountId", userDetails.getAccountId());
        }

        if (userDetails.getPhoneNumber() != null) {
            builder.claim("phoneNumber", userDetails.getPhoneNumber());
        }

        return builder.signWith(getSigningKey()).compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public List<String> getPrivilegesFromToken(String token) {
        String privStr = parseClaims(token).get("privileges", String.class);
        return privStr == null || privStr.isEmpty() ? List.of() : List.of(privStr.split(","));
    }

    public Long getAccountIdFromToken(String token) {
        Object accountId = parseClaims(token).get("accountId");
        return accountId != null ? Long.parseLong(accountId.toString()) : null;
    }

    public String getPhoneNumberFromToken(String token) {
        return parseClaims(token).get("phoneNumber", String.class);
    }

    public int getTokenVersionFromToken(String token) {
        Object version = parseClaims(token).get("tokenVersion");
        return version != null ? Integer.parseInt(version.toString()) : 0;
    }

    public boolean validateToken(String token) throws ExpiredJwtException {
        try {
            parseClaims(token);
            String username = getUsernameFromToken(token);
            int tokenVersion = getTokenVersionFromToken(token);
            int currentVersion = userDetailsService.getTokenVersion(username);

            if (tokenVersion < currentVersion) {
                log.debug("Версия токена устарела для пользователя {}: текущая версия токена={}, актуальная версия токена={}",
                        username, tokenVersion, currentVersion);
                return false;
            }

            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Срок действия JWT-токена истек: {}", e.getMessage());
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Недействительный JWT-токен: {}", e.getMessage());
            return false;
        }
    }
}
