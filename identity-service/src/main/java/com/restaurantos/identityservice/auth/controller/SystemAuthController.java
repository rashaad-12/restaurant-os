package com.restaurantos.identityservice.auth.controller;

import com.restaurantos.identityservice.auth.dto.ServiceTokenRequest;
import com.restaurantos.identityservice.auth.dto.ServiceTokenResponse;
import com.restaurantos.identityservice.auth.service.SystemAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Client-credentials endpoint for machine (service-to-service) callers. Sits under
 * {@code /auth-api/v1/auth/**}, which core-security permits without a bearer token — the caller
 * authenticates with its client id/secret and receives a short-lived SYSTEM access token.
 */
@RestController
@RequestMapping("auth-api/v1/auth/system")
@RequiredArgsConstructor
public class SystemAuthController {

    private final SystemAuthService systemAuthService;

    @PostMapping("/token")
    public ResponseEntity<ServiceTokenResponse> token(@RequestBody ServiceTokenRequest request) {
        return ResponseEntity.ok(systemAuthService.issueToken(request));
    }
}
