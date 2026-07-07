package com.restaurantos.coresecurity;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public final class TestKeys {

    public final String privatePem;
    public final String publicPem;

    private TestKeys(String privatePem, String publicPem) {
        this.privatePem = privatePem;
        this.publicPem = publicPem;
    }

    public static TestKeys generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            String priv = pem("PRIVATE KEY", pair.getPrivate().getEncoded());
            String pub = pem("PUBLIC KEY", pair.getPublic().getEncoded());
            return new TestKeys(priv, pub);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate test RSA keypair", e);
        }
    }

    private static String pem(String type, byte[] der) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";
    }
}
