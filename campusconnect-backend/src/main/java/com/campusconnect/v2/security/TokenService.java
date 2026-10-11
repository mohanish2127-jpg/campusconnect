package com.campusconnect.v2.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class TokenService {

    public record Claims(String role, String id, String name) {}

    private static final long TTL_MS = 12L * 60 * 60 * 1000;

    private final byte[] secret;

    public TokenService(@Value("${cc.token.secret:}") String configured) {
        if (configured == null || configured.isBlank()) {
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            this.secret = random;
        } else {
            this.secret = configured.getBytes(StandardCharsets.UTF_8);
        }
    }

    private static String b64(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static String enc(String text) {
        return b64(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String dec(String text) {
        return new String(Base64.getUrlDecoder().decode(text), StandardCharsets.UTF_8);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return b64(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public String issue(String role, String id, String name) {
        String payload = enc(role) + "." + enc(id) + "." + enc(name == null ? "" : name) + "."
                + (System.currentTimeMillis() + TTL_MS);
        return payload + "." + sign(payload);
    }

    public Claims verify(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in first");
        }
        String[] parts = header.substring(7).trim().split("\\.");
        if (parts.length != 5) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session");
        }
        String payload = String.join(".", parts[0], parts[1], parts[2], parts[3]);
        boolean ok = MessageDigest.isEqual(
                sign(payload).getBytes(StandardCharsets.UTF_8), parts[4].getBytes(StandardCharsets.UTF_8));
        try {
            if (!ok || Long.parseLong(parts[3]) < System.currentTimeMillis()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expired, please log in again");
            }
            return new Claims(dec(parts[0]), dec(parts[1]), dec(parts[2]));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session");
        }
    }

    public Claims require(String header, String role) {
        Claims claims = verify(header);
        if (role != null && !role.equals(claims.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a " + role + " can do this");
        }
        return claims;
    }
}
