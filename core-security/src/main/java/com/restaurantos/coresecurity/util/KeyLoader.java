package com.restaurantos.coresecurity.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class KeyLoader {

    private static final String CLASSPATH_PREFIX = "classpath:";

    private KeyLoader() {
    }

    public static RSAPublicKey loadPublicKey(String location) {
        byte[] der = decode(readPem(location), "PUBLIC KEY");
        try {
            return (RSAPublicKey) keyFactory().generatePublic(new X509EncodedKeySpec(der));
        } catch (InvalidKeySpecException e) {
            throw new IllegalStateException("Invalid RSA public key at " + location, e);
        }
    }

    public static RSAPrivateKey loadPrivateKey(String location) {
        byte[] der = decode(readPem(location), "PRIVATE KEY");
        try {
            return (RSAPrivateKey) keyFactory().generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (InvalidKeySpecException e) {
            throw new IllegalStateException("Invalid RSA private key at " + location, e);
        }
    }

    private static String readPem(String location) {
        if (location == null || location.isBlank()) {
            throw new IllegalStateException("Key location is not configured");
        }
        if (location.startsWith("-----BEGIN")) {
            return location;
        }
        if (location.startsWith(CLASSPATH_PREFIX)) {
            String resource = location.substring(CLASSPATH_PREFIX.length());
            try (InputStream in = KeyLoader.class.getClassLoader().getResourceAsStream(resource)) {
                if (in == null) {
                    throw new IllegalStateException("Key resource not found on classpath: " + resource);
                }
                return new String(in.readAllBytes(), UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read key resource: " + resource, e);
            }
        }
        try {
            return Files.readString(Path.of(location));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read key file: " + location, e);
        }
    }

    private static byte[] decode(String pem, String type) {
        String base64 = pem
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }

    private static KeyFactory keyFactory() {
        try {
            return KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA KeyFactory unavailable", e);
        }
    }
}
