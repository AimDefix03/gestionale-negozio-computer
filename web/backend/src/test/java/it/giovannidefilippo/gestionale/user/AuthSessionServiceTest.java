package it.giovannidefilippo.gestionale.user;

import it.giovannidefilippo.gestionale.common.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AuthSessionServiceTest {
    @Autowired
    private UserService userService;

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    private AuthSessionRepository sessionRepository;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private SecurityMaintenanceService securityMaintenanceService;

    @Autowired
    private SecurityMetricsService securityMetricsService;

    @Test
    void createsPersistentSessionWithHashedTokenAndRevokesOnLogout() {
        String username = "session_" + UUID.randomUUID().toString().replace("-", "");
        userService.registerPublic(username, "Client123!", UserRole.CUSTOMER);
        UserResponse user = userService.login(username, "Client123!", UserRole.CUSTOMER);

        AuthSessionResponse response = authSessionService.create(user);

        assertThat(response.token()).isNotBlank();
        assertThat(response.expiresAt()).isAfter(Instant.now());

        AuthenticatedUser authenticatedUser = authSessionService.require(response.token());
        assertThat(authenticatedUser.username()).isEqualTo(username);
        assertThat(authenticatedUser.role()).isEqualTo(UserRole.CUSTOMER);

        List<AuthSession> sessions = sessionRepository.findByUsernameIgnoreCaseOrderByCreatedAtDesc(username);
        assertThat(sessions).hasSize(1);

        AuthSession storedSession = sessions.get(0);
        assertThat(storedSession.getTokenHash()).isNotBlank();
        assertThat(storedSession.getTokenHash()).isNotEqualTo(response.token());
        assertThat(storedSession.getRevokedAt()).isNull();

        authSessionService.logout(response.token());

        AuthSession revokedSession = sessionRepository.findById(storedSession.getId()).orElseThrow();
        assertThat(revokedSession.getRevokedAt()).isNotNull();
        assertThatThrownBy(() -> authSessionService.require(response.token()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Sessione non valida");
    }

    @Test
    void persistsFailedLoginAttemptsAndClearsThemAfterSuccessfulLogin() {
        String username = "locked_" + UUID.randomUUID().toString().replace("-", "");

        for (int index = 0; index < 5; index++) {
            authSessionService.registerFailedLogin(username);
        }

        LoginAttempt attempt = loginAttemptRepository.findByUsernameKey(username).orElseThrow();
        assertThat(attempt.getAttempts()).isEqualTo(5);
        assertThat(attempt.getLockedUntil()).isAfter(Instant.now());

        assertThatThrownBy(() -> authSessionService.assertLoginAllowed(username))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Troppi tentativi");

        authSessionService.clearFailedLogin(username);

        assertThat(loginAttemptRepository.findByUsernameKey(username)).isEmpty();
        authSessionService.assertLoginAllowed(username);
    }

    @Test
    void rotatesSessionTokenAndImmediatelyRevokesPreviousToken() {
        String username = "rotation_" + UUID.randomUUID().toString().replace("-", "");
        userService.registerPublic(username, "Client123!", UserRole.CUSTOMER);
        UserResponse user = userService.login(username, "Client123!", UserRole.CUSTOMER);
        AuthSessionResponse original = authSessionService.create(user);

        AuthSessionResponse rotated = authSessionService.rotate(original.token());

        assertThat(rotated.token()).isNotEqualTo(original.token());
        assertThat(rotated.expiresAt()).isAfterOrEqualTo(original.expiresAt());
        assertThatThrownBy(() -> authSessionService.require(original.token()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Sessione non valida");
        assertThat(authSessionService.require(rotated.token()).username()).isEqualTo(username);

        List<AuthSession> sessions = sessionRepository.findByUsernameIgnoreCaseOrderByCreatedAtDesc(username);
        assertThat(sessions).hasSize(2);
        assertThat(sessions).anyMatch(session -> session.getRevokedAt() != null);
        assertThat(sessions).anyMatch(session -> session.getRevokedAt() == null && session.getLastUsedAt() != null);
    }

    @Test
    void rejectsSecondRotationWithAlreadyConsumedToken() {
        String username = "single_rotation_" + UUID.randomUUID().toString().replace("-", "");
        userService.registerPublic(username, "Client123!", UserRole.CUSTOMER);
        AuthSessionResponse original = authSessionService.create(userService.login(username, "Client123!", UserRole.CUSTOMER));

        authSessionService.rotate(original.token());

        assertThatThrownBy(() -> authSessionService.rotate(original.token()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Sessione non valida");
    }

    @Test
    void expiresSessionAtAbsoluteAndIdleBoundaries() {
        Instant createdAt = Instant.parse("2026-07-15T08:00:00Z");
        AuthSession session = new AuthSession("boundary-" + UUID.randomUUID(), "boundary_user", UserRole.CUSTOMER, createdAt, createdAt.plus(Duration.ofMinutes(45)));

        assertThat(session.isExpired(createdAt.plus(Duration.ofMinutes(45)).minusMillis(1))).isFalse();
        assertThat(session.isExpired(createdAt.plus(Duration.ofMinutes(45)))).isTrue();
        assertThat(session.isIdleExpired(createdAt.plus(Duration.ofMinutes(30)).minusMillis(1), Duration.ofMinutes(30))).isFalse();
        assertThat(session.isIdleExpired(createdAt.plus(Duration.ofMinutes(30)), Duration.ofMinutes(30))).isTrue();
    }

    @Test
    void securityMetricsExcludeSessionsExpiredByInactivity() {
        long activeBefore = securityMetricsService.snapshot().activeSessions();
        Instant now = Instant.now();

        sessionRepository.save(new AuthSession(
                "idle-metric-" + UUID.randomUUID(),
                "idle_metric_user",
                UserRole.CUSTOMER,
                now.minus(Duration.ofMinutes(31)),
                now.plus(Duration.ofMinutes(14))
        ));

        assertThat(securityMetricsService.snapshot().activeSessions()).isEqualTo(activeBefore);

        sessionRepository.save(new AuthSession(
                "active-metric-" + UUID.randomUUID(),
                "active_metric_user",
                UserRole.CUSTOMER,
                now,
                now.plus(Duration.ofMinutes(45))
        ));

        assertThat(securityMetricsService.snapshot().activeSessions()).isEqualTo(activeBefore + 1);
    }

    @Test
    void cleanupRemovesOldSessionsAndExpiredLoginAttempts() {
        Instant old = Instant.now().minus(Duration.ofDays(10));
        AuthSession oldSession = sessionRepository.save(new AuthSession("old-session-" + UUID.randomUUID(), "utente_obsoleto", UserRole.CUSTOMER, old, old));

        String usernameKey = "expired_lock_" + UUID.randomUUID().toString().replace("-", "");
        LoginAttempt expiredAttempt = new LoginAttempt(usernameKey, old);
        for (int index = 0; index < 5; index++) {
            expiredAttempt.registerFailure(old, 5, Duration.ofMinutes(5));
        }
        loginAttemptRepository.save(expiredAttempt);

        securityMaintenanceService.cleanup();

        assertThat(sessionRepository.findById(oldSession.getId())).isEmpty();
        assertThat(loginAttemptRepository.findByUsernameKey(usernameKey)).isEmpty();
    }
}
