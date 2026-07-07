package com.restaurantos.coresecurity.config;

import com.restaurantos.coresecurity.enums.TokenType;
import com.restaurantos.coresecurity.model.AuthenticatedUser;
import com.restaurantos.coresecurity.revocation.TokenRevocationChecker;
import com.restaurantos.coresecurity.service.JwtService;
import com.restaurantos.coresecurity.util.JwtTokenUtil;
import com.restaurantos.coresecurity.util.SecurityAuthorities;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final TokenRevocationChecker revocationChecker;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = JwtTokenUtil.extractToken(request);

        if (isBlank(token) || nonNull(SecurityContextHolder.getContext().getAuthentication())) {
            filterChain.doFilter(request, response);
            return;
        }

        jwtService.tryVerify(token)
                .filter(this::isUsableAccessToken)
                .ifPresent(user -> authenticate(user, request));

        filterChain.doFilter(request, response);
    }

    private boolean isUsableAccessToken(AuthenticatedUser user) {
        if (user.getTokenType() != TokenType.ACCESS) {
            log.debug("Ignoring non-access token for {}", user.getUsername());
            return false;
        }
        if (nonNull(user.getTokenId()) && revocationChecker.isRevoked(user.getTokenId())) {
            log.debug("Ignoring revoked token {}", user.getTokenId());
            return false;
        }
        return true;
    }

    private void authenticate(AuthenticatedUser user, HttpServletRequest request) {
        if (isNull(user.getUsername())) return;

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user, null, SecurityAuthorities.toAuthorities(user.getRoles()));
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(auth);
        log.debug("Authenticated {} with roles {}", user.getUsername(), user.getRoles());
    }
}
