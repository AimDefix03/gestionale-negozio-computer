package it.giovannidefilippo.gestionale.common;

import it.giovannidefilippo.gestionale.user.UserRole;
import it.giovannidefilippo.gestionale.user.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
class SeedAdminInitializer implements CommandLineRunner {
    private static final String DEFAULT_LOCAL_USERNAME = "admin";
    private static final String DEFAULT_LOCAL_PASSWORD_SHA_256 =
            "0b6ac2ff5d25df4074c23e1cc44689786ae67d148194cae8b4c16fe14ce2feb6";

    private final UserService userService;
    private final boolean enabled;
    private final String username;
    private final String password;

    SeedAdminInitializer(
            UserService userService,
            @Value("${gestionale.bootstrap.super-admin.enabled:false}") boolean enabled,
            @Value("${gestionale.bootstrap.super-admin.username:}") String username,
            @Value("${gestionale.bootstrap.super-admin.password:}") String password
    ) {
        this.userService = userService;
        this.enabled = enabled;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (userService.hasAccounts()) {
            return;
        }
        if (!enabled) {
            throw new IllegalStateException("Nessun account presente. Abilita il bootstrap super admin con GESTIONALE_BOOTSTRAP_SUPER_ADMIN_ENABLED=true e credenziali sicure, poi disabilitalo dopo il primo avvio.");
        }
        validateBootstrapConfiguration();
        userService.createAccount(username, password, UserRole.SUPER_ADMIN, "Sistema", "Bootstrap super admin iniziale da configurazione ambiente");
    }

    private void validateBootstrapConfiguration() {
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("Configura GESTIONALE_BOOTSTRAP_SUPER_ADMIN_USERNAME per inizializzare il super admin.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Configura GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD per inizializzare il super admin.");
        }
        if (DEFAULT_LOCAL_USERNAME.equalsIgnoreCase(username.trim()) || matchesDefaultLocalPassword(password)) {
            throw new IllegalStateException("Le credenziali locali di default non possono essere usate per il bootstrap.");
        }
        if (password.length() < 12 || !password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*") || !password.matches(".*[0-9].*") || !password.matches(".*[^A-Za-z0-9].*")) {
            throw new IllegalStateException("La password del super admin deve avere almeno 12 caratteri, maiuscole, minuscole, numeri e simboli.");
        }
    }

    private boolean matchesDefaultLocalPassword(String candidate) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(candidate.getBytes(StandardCharsets.UTF_8));
            byte[] knownDigest = HexFormat.of().parseHex(DEFAULT_LOCAL_PASSWORD_SHA_256);
            return MessageDigest.isEqual(digest, knownDigest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 non disponibile per la validazione del bootstrap.", exception);
        }
    }
}
