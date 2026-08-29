package com.example.sbp.security;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String jwt = getJwtFromRequest(request);

        if (!StringUtils.hasText(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromToken(jwt);
                String role = tokenProvider.getRoleFromToken(jwt);
                List<String> privilegesStr = tokenProvider.getPrivilegesFromToken(jwt);
                Long accountId = tokenProvider.getAccountIdFromToken(jwt);
                String phoneNumber = tokenProvider.getPhoneNumberFromToken(jwt);
                int tokenVersion = tokenProvider.getTokenVersionFromToken(jwt);

                Set<Privilege> privileges = new HashSet<>();
                for (String privilege : privilegesStr) {
                    try {
                        privileges.add(Privilege.valueOf(privilege));
                    } catch (IllegalArgumentException ignored) {}
                }

                CustomUserDetails userDetails = new CustomUserDetails(
                        username, "", role, privileges, accountId, phoneNumber, tokenVersion
                );

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                request.setAttribute("jwt_expired", true);
            }
        } catch (ExpiredJwtException ex) {
            request.setAttribute("jwt_expired", true);
        } catch (Exception ex) {
            logger.error("Auth error", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
