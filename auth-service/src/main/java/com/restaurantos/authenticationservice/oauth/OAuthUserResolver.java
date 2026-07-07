package com.restaurantos.authenticationservice.oauth;

import com.restaurantos.coresecurity.enums.Audience;
import com.restaurantos.userservice.model.User;

/**
 * Maps a verified {@link OAuthIdentity} to a platform {@link User}. Pluggable so
 * the matching/provisioning strategy can evolve (e.g. call user-service over HTTP
 * once auth-service no longer shares the user datastore).
 */
public interface OAuthUserResolver {

    User resolve(OAuthIdentity identity, Audience audience);
}
