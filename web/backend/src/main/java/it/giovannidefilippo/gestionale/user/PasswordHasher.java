package it.giovannidefilippo.gestionale.user;

import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
class PasswordHasher {
    private final PasswordEncoder passwordEncoder;

    PasswordHasher(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    PasswordHash hash(String password) {
        return new PasswordHash("", passwordEncoder.encode(password));
    }

    boolean matches(String password, String salt, String expectedHash) {
        return passwordEncoder.matches(password, expectedHash);
    }

    record PasswordHash(String salt, String hash) {
    }
}
