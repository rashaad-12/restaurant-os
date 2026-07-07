package com.restaurantos.coresecurity.service.impl;

import com.restaurantos.coresecurity.TestKeys;
import com.restaurantos.coresecurity.config.SecurityProperties;
import com.restaurantos.coresecurity.enums.Audience;
import com.restaurantos.coresecurity.enums.TokenType;
import com.restaurantos.coresecurity.exception.InvalidTokenException;
import com.restaurantos.coresecurity.model.AuthenticatedUser;
import com.restaurantos.coresecurity.model.TokenRequest;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceImplTest {

    private static final TestKeys KEYS = TestKeys.generate();

    private JwtServiceImpl issuerService() {
        return newService(KEYS.publicPem, KEYS.privatePem, "restaurant-os");
    }

    private JwtServiceImpl newService(String publicPem, String privatePem, String issuer) {
        SecurityProperties props = new SecurityProperties();
        props.setIssuer(issuer);
        props.setPublicKey(publicPem);
        props.setPrivateKey(privatePem);
        JwtServiceImpl service = new JwtServiceImpl(props);
        service.init();
        return service;
    }

    private TokenRequest staffRequest() {
        return new TokenRequest("alice@example.com", Set.of("MANAGER"), Set.of("R1"), Audience.STAFF);
    }

    @Test
    void issueThenVerify_roundTripsAllClaims() {
        JwtServiceImpl service = issuerService();

        String token = service.issue(staffRequest(), TokenType.ACCESS);
        AuthenticatedUser user = service.verify(token);

        assertThat(user.getUsername()).isEqualTo("alice@example.com");
        assertThat(user.getRoles()).containsExactly("MANAGER");
        assertThat(user.getRestaurantCodes()).containsExactly("R1");
        assertThat(user.getAudience()).isEqualTo(Audience.STAFF);
        assertThat(user.getTokenType()).isEqualTo(TokenType.ACCESS);
        assertThat(user.getTokenId()).isNotBlank();
    }

    @Test
    void refreshToken_carriesRefreshType() {
        JwtServiceImpl service = issuerService();

        AuthenticatedUser user = service.verify(service.issue(staffRequest(), TokenType.REFRESH));

        assertThat(user.getTokenType()).isEqualTo(TokenType.REFRESH);
    }

    @Test
    void tamperedToken_isRejected() {
        JwtServiceImpl service = issuerService();
        String token = service.issue(staffRequest(), TokenType.ACCESS);

        String tampered = token.substring(0, token.length() - 3) + "abc";

        assertThatThrownBy(() -> service.verify(tampered))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void tokenFromAnotherIssuer_isRejected() {
        String token = issuerService().issue(staffRequest(), TokenType.ACCESS);

        JwtServiceImpl strangerVerifier = newService(KEYS.publicPem, KEYS.privatePem, "some-other-issuer");

        assertThatThrownBy(() -> strangerVerifier.verify(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void verifyOnlyService_cannotIssue() {
        JwtServiceImpl verifyOnly = newService(KEYS.publicPem, null, "restaurant-os");

        assertThatThrownBy(() -> verifyOnly.issue(staffRequest(), TokenType.ACCESS))
                .isInstanceOf(IllegalStateException.class);
    }
}
