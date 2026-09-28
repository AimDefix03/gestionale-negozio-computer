package it.giovannidefilippo.gestionale.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PasswordHasherTest {
    @Autowired
    private PasswordHasher passwordHasher;

    @Test
    void hashesPasswordsWithBCrypt() {
        PasswordHasher.PasswordHash hash = passwordHasher.hash("Admin123!");

        assertThat(hash.hash()).startsWith("$2");
        assertThat(hash.hash()).isNotEqualTo("Admin123!");
        assertThat(passwordHasher.matches("Admin123!", hash.salt(), hash.hash())).isTrue();
        assertThat(passwordHasher.matches("wrong-password", hash.salt(), hash.hash())).isFalse();
    }
}
