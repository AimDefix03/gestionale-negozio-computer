package it.giovannidefilippo.gestionale.user;

import it.giovannidefilippo.gestionale.common.UnauthorizedException;
import it.giovannidefilippo.gestionale.common.PostgreSqlIntegrationTestSupport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Tag("postgresql")
class AuthSessionServiceTest extends PostgreSqlIntegrationTestSupport {
    @Autowired
    private UserService userService;

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    private AuthSessionRepository sessionRepository;

    @Autowired
    private UserAccountRepository accountRepository;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private SecurityMaintenanceService securityMaintenanceService;

    @Autowired
    private SecurityMetricsService securityMetricsService;

    @Test
    void permissionChecksUseWritableTransactionsForSessionLockingAndTouch() throws NoSuchMethodException {
        TransactionAttribute transaction = new AnnotationTransactionAttributeSource().getTransactionAttribute(
                AuthSessionService.class.getMethod("requirePermission", String.class, UserPermission.class),
                AuthSessionService.class
        );

        assertThat(transaction).isNotNull();
        assertThat(transaction.isReadOnly()).isFalse();
    }

    @Test
    void createsPersistentSessionWithHashedTokenAndRevokesOnLogout() {
        String username = "session_" + UUID.randomUUID().toString().replace("-", "");
        userService.registerPublic(username, "Client123!");
        UserResponse user = userService.login(username, "Client123!");

        AuthSessionResponse response = authSessionService.create(user);

        assertThat(response.token()).isNotBlank();
        assertThat(response.expiresAt()).isAfter(Instant.now());

        AuthenticatedUser authenticatedUser = authSessionService.require(response.token());
        assertThat(authenticatedUser.username()).isEqualTo(username);
        assertThat(authenticatedUser.role()).isEqualTo(UserRole.CUSTOMER);

        List<AuthSession> sessions = sessionRepository.findByAccountIdOrderByCreatedAtDesc(user.id());
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
    void concurrentFailedLoginsAreCountedWithoutLostUpdates() throws Exception {
        String username = "concurrent_lock_" + UUID.randomUUID().toString().replace("-", "");
        int failures = 8;
        ExecutorService executor = Executors.newFixedThreadPool(failures);
        CountDownLatch ready = new CountDownLatch(failures);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<Object>> results = java.util.stream.IntStream.range(0, failures)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        start.await(5, TimeUnit.SECONDS);
                        authSessionService.registerFailedLogin(username);
                        return null;
                    }))
                    .toList();

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<Object> result : results) {
                result.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        LoginAttempt attempt = loginAttemptRepository.findByUsernameKey(username).orElseThrow();
        assertThat(attempt.getAttempts()).isEqualTo(failures);
        assertThat(attempt.getLockedUntil()).isAfter(Instant.now());
    }

    @Test
    void failedLoginTimelineRemainsMonotonicWhenRequestsCompleteOutOfOrder() {
        Instant latestRequest = Instant.parse("2026-08-06T10:00:30Z");
        LoginAttempt attempt = new LoginAttempt("out_of_order", latestRequest);

        for (int index = 0; index < 5; index++) {
            attempt.registerFailure(latestRequest, 5, Duration.ofMinutes(5));
        }
        attempt.registerFailure(latestRequest.minusSeconds(30), 5, Duration.ofMinutes(5));

        assertThat(attempt.getAttempts()).isEqualTo(6);
        assertThat(attempt.getLastAttemptAt()).isEqualTo(latestRequest);
        assertThat(attempt.getLockedUntil()).isEqualTo(latestRequest.plus(Duration.ofMinutes(5)));
    }

    @Test
    void rotatesSessionTokenAndImmediatelyRevokesPreviousToken() {
        String username = "rotation_" + UUID.randomUUID().toString().replace("-", "");
        userService.registerPublic(username, "Client123!");
        UserResponse user = userService.login(username, "Client123!");
        AuthSessionResponse original = authSessionService.create(user);

        AuthSessionResponse rotated = authSessionService.rotate(original.token());

        assertThat(rotated.token()).isNotEqualTo(original.token());
        assertThat(rotated.expiresAt()).isAfterOrEqualTo(original.expiresAt());
        assertThatThrownBy(() -> authSessionService.require(original.token()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Sessione non valida");
        assertThat(authSessionService.require(rotated.token()).username()).isEqualTo(username);

        List<AuthSession> sessions = sessionRepository.findByAccountIdOrderByCreatedAtDesc(user.id());
        assertThat(sessions).hasSize(2);
        assertThat(sessions).anyMatch(session -> session.getRevokedAt() != null);
        assertThat(sessions).anyMatch(session -> session.getRevokedAt() == null && session.getLastUsedAt() != null);
    }

    @Test
    void rejectsSecondRotationWithAlreadyConsumedToken() {
        String username = "single_rotation_" + UUID.randomUUID().toString().replace("-", "");
        userService.registerPublic(username, "Client123!");
        AuthSessionResponse original = authSessionService.create(userService.login(username, "Client123!"));

        authSessionService.rotate(original.token());

        assertThatThrownBy(() -> authSessionService.rotate(original.token()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Sessione non valida");
    }

    @Test
    void expiresSessionAtAbsoluteAndIdleBoundaries() {
        Instant createdAt = Instant.parse("2026-07-15T08:00:00Z");
        UserAccount account = detachedAccount("boundary_user");
        AuthSession session = new AuthSession("boundary-" + UUID.randomUUID(), account, createdAt, createdAt.plus(Duration.ofMinutes(45)));

        assertThat(session.isExpired(createdAt.plus(Duration.ofMinutes(45)).minusMillis(1))).isFalse();
        assertThat(session.isExpired(createdAt.plus(Duration.ofMinutes(45)))).isTrue();
        assertThat(session.isIdleExpired(createdAt.plus(Duration.ofMinutes(30)).minusMillis(1), Duration.ofMinutes(30))).isFalse();
        assertThat(session.isIdleExpired(createdAt.plus(Duration.ofMinutes(30)), Duration.ofMinutes(30))).isTrue();
    }

    @Test
    void securityMetricsExcludeSessionsExpiredByInactivity() {
        long activeBefore = securityMetricsService.snapshot().activeSessions();
        Instant now = Instant.now();
        UserResponse idleUser = userService.registerPublic(
                "idle_metric_" + UUID.randomUUID().toString().replace("-", ""),
                "Client123!"
        );

        sessionRepository.save(new AuthSession(
                "idle-metric-" + UUID.randomUUID(),
                accountRepository.findById(idleUser.id()).orElseThrow(),
                now.minus(Duration.ofMinutes(31)),
                now.plus(Duration.ofMinutes(14))
        ));

        assertThat(securityMetricsService.snapshot().activeSessions()).isEqualTo(activeBefore);

        UserResponse activeUser = userService.registerPublic(
                "active_metric_" + UUID.randomUUID().toString().replace("-", ""),
                "Client123!"
        );
        sessionRepository.save(new AuthSession(
                "active-metric-" + UUID.randomUUID(),
                accountRepository.findById(activeUser.id()).orElseThrow(),
                now,
                now.plus(Duration.ofMinutes(45))
        ));

        assertThat(securityMetricsService.snapshot().activeSessions()).isEqualTo(activeBefore + 1);
    }

    @Test
    void cleanupRemovesOldSessionsAndExpiredLoginAttempts() {
        Instant old = Instant.now().minus(Duration.ofDays(10));
        UserResponse oldUser = userService.registerPublic(
                "utente_obsoleto_" + UUID.randomUUID().toString().replace("-", ""),
                "Client123!"
        );
        AuthSession oldSession = sessionRepository.save(new AuthSession(
                "old-session-" + UUID.randomUUID(),
                accountRepository.findById(oldUser.id()).orElseThrow(),
                old,
                old
        ));

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

    private UserAccount detachedAccount(String username) {
        return new UserAccount(
                username,
                "salt",
                "hash",
                UserRole.CUSTOMER,
                AccountProvisioningSource.SELF_SERVICE,
                true,
                Instant.now(),
                username
        );
    }
}
