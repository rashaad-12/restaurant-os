package com.restaurantos.identityservice.auth.oauth;

import com.restaurantos.coresecurity.enums.Audience;
import com.restaurantos.identityservice.user.model.User;

public interface OAuthUserResolver {

    User resolve(OAuthIdentity identity, Audience audience);
}
