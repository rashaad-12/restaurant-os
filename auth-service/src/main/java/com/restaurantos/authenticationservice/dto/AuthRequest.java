package com.restaurantos.authenticationservice.dto;

import com.restaurantos.coresecurity.enums.Audience;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

import static com.restaurantos.coresecurity.enums.Audience.STAFF;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    private Audience audience;

    /** OAuth provider key (e.g. "google", "apple") — used by CUSTOMER/PARTNER flows. */
    private String authProvider;

    /** OIDC ID token from the provider — verified server-side for CUSTOMER/PARTNER flows. */
    private String idToken;

    /** Username — used by the STAFF (internal password) flow. */
    private String username;

    /** Password — used by the STAFF (internal password) flow. */
    private String password;

    private Set<String> roles;

    public boolean isStaff() {
        return this.audience == STAFF;
    }

}
