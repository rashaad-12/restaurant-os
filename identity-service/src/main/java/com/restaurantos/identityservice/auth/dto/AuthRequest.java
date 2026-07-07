package com.restaurantos.identityservice.auth.dto;

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

    private String authProvider;

    private String idToken;

    private String username;

    private String password;

    private Set<String> roles;

    public boolean isStaff() {
        return this.audience == STAFF;
    }

}
