package com.restaurantos.coresecurity.config;

import com.restaurantos.coresecurity.TestKeys;
import com.restaurantos.coresecurity.enums.Audience;
import com.restaurantos.coresecurity.enums.TokenType;
import com.restaurantos.coresecurity.model.AuthenticatedUser;
import com.restaurantos.coresecurity.model.TokenRequest;
import com.restaurantos.coresecurity.revocation.TokenRevocationChecker;
import com.restaurantos.coresecurity.service.impl.JwtServiceImpl;
import com.restaurantos.coresecurity.service.impl.TestJwtServices;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private static final TestKeys KEYS = TestKeys.generate();

    private JwtServiceImpl jwtService;

    @BeforeEach
    void setUp() {
        jwtService = TestJwtServices.issuer(KEYS);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private String token(TokenType type) {
        TokenRequest request = new TokenRequest("alice@example.com", Set.of("MANAGER"), Set.of("R1"), Audience.STAFF);
        return jwtService.issue(request, type);
    }

    private Authentication runFilterWithToken(String token, TokenRevocationChecker revocationChecker) throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, revocationChecker);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).as("filter must always continue the chain").isNotNull();
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void accessToken_authenticatesTheRequest() throws Exception {
        Authentication auth = runFilterWithToken(token(TokenType.ACCESS), id -> false);

        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        assertThat(((AuthenticatedUser) auth.getPrincipal()).getUsername()).isEqualTo("alice@example.com");
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_MANAGER");
    }

    @Test
    void refreshToken_isNotAcceptedAsBearerCredential() throws Exception {
        Authentication auth = runFilterWithToken(token(TokenType.REFRESH), id -> false);

        assertThat(auth).isNull();
    }

    @Test
    void revokedToken_doesNotAuthenticate() throws Exception {
        Authentication auth = runFilterWithToken(token(TokenType.ACCESS), id -> true);

        assertThat(auth).isNull();
    }
}
