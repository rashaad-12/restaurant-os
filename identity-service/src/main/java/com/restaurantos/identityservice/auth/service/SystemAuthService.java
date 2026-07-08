package com.restaurantos.identityservice.auth.service;

import com.restaurantos.identityservice.auth.dto.ServiceTokenRequest;
import com.restaurantos.identityservice.auth.dto.ServiceTokenResponse;

public interface SystemAuthService {

    ServiceTokenResponse issueToken(ServiceTokenRequest request);
}
