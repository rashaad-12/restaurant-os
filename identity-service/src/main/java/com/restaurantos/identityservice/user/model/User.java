package com.restaurantos.identityservice.user.model;

import com.restaurantos.identityservice.user.enums.EntityStatus;
import com.restaurantos.identityservice.user.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;

import static com.restaurantos.identityservice.user.enums.EntityStatus.ACTIVE;
import static com.restaurantos.identityservice.user.enums.EntityStatus.ARCHIVED;
import static com.restaurantos.identityservice.user.enums.EntityStatus.SUSPENDED;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User implements UserDetails {

    @Id
    private String id;

    private String username;

    private String password;

    private String firstName;

    private String lastName;

    private Set<UserRole> roles;

    private String oauthProvider;

    private Set<String> restaurantCodes;

    private Set<String> restaurantGroups;

    private EntityStatus status;

    private String createdBy;

    @CreatedDate
    private LocalDateTime createDttm;

    @LastModifiedDate
    private LocalDateTime updateDttm;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles;
    }

    @Override
    public boolean isAccountNonExpired() {
        return !ARCHIVED.equals(status);
    }

    @Override
    public boolean isAccountNonLocked() {
        return !SUSPENDED.equals(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return ACTIVE.equals(status);
    }
}
