package com.restaurantos.coresecurity.service.impl;

import com.restaurantos.coresecurity.TestKeys;
import com.restaurantos.coresecurity.config.SecurityProperties;

public final class TestJwtServices {

    private TestJwtServices() {
    }

    public static JwtServiceImpl issuer(TestKeys keys) {
        SecurityProperties props = new SecurityProperties();
        props.setPublicKey(keys.publicPem);
        props.setPrivateKey(keys.privatePem);
        JwtServiceImpl service = new JwtServiceImpl(props);
        service.init();
        return service;
    }
}
