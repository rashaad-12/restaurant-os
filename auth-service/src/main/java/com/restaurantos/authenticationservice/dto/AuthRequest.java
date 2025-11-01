package com.restaurantos.authenticationservice.dto;

import com.restaurantos.coresecurity.enums.AuthType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

import static com.restaurantos.coresecurity.enums.AuthType.CUSTOMER;
import static com.restaurantos.coresecurity.enums.AuthType.INTERNAL;
import static com.restaurantos.coresecurity.enums.AuthType.OTP;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    private AuthType authType;

    private String authProvider;

    private String username;

    private String password;

    private Set<String> roles;

    public boolean isInternal() {
        return this.authType == INTERNAL;
    }

    public boolean isCustomer() {
        return this.authType == CUSTOMER;
    }

    public boolean isOTP() {
        return this.authType == OTP;
    }

}
