package com.restaurantos.authenticationservice.service.impl;

import com.restaurantos.authenticationservice.dto.AuthRequest;
import com.restaurantos.authenticationservice.service.CustomerAuthService;
import com.restaurantos.authenticationservice.util.CookieUtil;
import com.restaurantos.coresecurity.config.SecurityProperties;
import com.restaurantos.coresecurity.service.JwtService;
import com.restaurantos.userservice.model.User;
import com.restaurantos.userservice.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

import static com.restaurantos.coresecurity.enums.AuthType.CUSTOMER;

@Service
public class CustomerAuthServiceImpl implements CustomerAuthService {

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Override
    public void authenticate(AuthRequest request, HttpServletResponse response) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        //TODO: implement proper OAuth provider verification
        if (!Objects.equals(user.getOauthProvider(), request.getAuthProvider())) {
            throw new BadCredentialsException("OAuth provider mismatch");
        }

        Map<String, Object> claims = CookieUtil.buildClaims(user);
        String accessToken = jwtService.generateToken(CUSTOMER, user.getUsername(), claims, true);
        String refreshToken = jwtService.generateToken(CUSTOMER, user.getUsername(), claims, false);

        SecurityProperties.JwtProperties config = securityProperties.get(CUSTOMER);
        CookieUtil.setAuthCookies(response, accessToken, refreshToken, config.getAccessTokenTtl(), config.getRefreshTokenTtl());
    }
}




