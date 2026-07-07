package com.restaurantos.authenticationservice.oauth.impl;

import com.restaurantos.authenticationservice.config.OAuthProperties;
import com.restaurantos.authenticationservice.exception.OAuthAuthenticationException;
import com.restaurantos.authenticationservice.oauth.OAuthIdentity;
import com.restaurantos.authenticationservice.oauth.OAuthUserResolver;
import com.restaurantos.coresecurity.enums.Audience;
import com.restaurantos.userservice.enums.EntityStatus;
import com.restaurantos.userservice.enums.UserRole;
import com.restaurantos.userservice.model.User;
import com.restaurantos.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Default resolver: matches an existing account by the provider-verified email,
 * enforces that the account is linked to the same provider, and optionally
 * just-in-time provisions a new account when {@code app.oauth.provisioning.enabled}.
 *
 * <p>NOTE: this writes to the shared user datastore via {@code UserRepository}.
 * When auth-service is split from user-service, provisioning should call the
 * user-service API instead — this class is the single seam to change.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultOAuthUserResolver implements OAuthUserResolver {

    private final UserRepository userRepository;

    private final OAuthProperties properties;

    @Override
    public User resolve(OAuthIdentity identity, Audience audience) {
        if (isBlank(identity.getEmail()) || !identity.isEmailVerified()) {
            throw new OAuthAuthenticationException("Provider did not supply a verified email");
        }

        return userRepository.findByUsername(identity.getEmail())
                .map(user -> ensureProviderLinked(user, identity))
                .orElseGet(() -> provision(identity, audience));
    }

    private User ensureProviderLinked(User user, OAuthIdentity identity) {
        if (isNotBlank(user.getOauthProvider())
                && !user.getOauthProvider().equalsIgnoreCase(identity.getProvider())) {
            throw new OAuthAuthenticationException("Account is linked to a different sign-in provider");
        }
        return user;
    }

    private User provision(OAuthIdentity identity, Audience audience) {
        if (!properties.getProvisioning().isEnabled()) {
            throw new OAuthAuthenticationException("No account exists for this identity");
        }

        User user = User.builder()
                .username(identity.getEmail())
                .firstName(identity.getName())
                .oauthProvider(identity.getProvider())
                .roles(Set.of(defaultRole(audience)))
                .status(defaultStatus(audience))
                .build();

        log.info("Provisioning new {} account for {} via {}", audience, identity.getEmail(), identity.getProvider());
        return userRepository.save(user);
    }

    private UserRole defaultRole(Audience audience) {
        return audience == Audience.PARTNER ? UserRole.DELIVERY_PARTNER : UserRole.CUSTOMER;
    }

    private EntityStatus defaultStatus(Audience audience) {
        // Delivery partners must be vetted before they can operate; customers self-activate.
        return audience == Audience.PARTNER ? EntityStatus.PENDING_APPROVAL : EntityStatus.ACTIVE;
    }
}
