package it.giovannidefilippo.gestionale.user;

import it.giovannidefilippo.gestionale.common.UnauthorizedException;
import it.giovannidefilippo.gestionale.common.PostgreSqlIntegrationTestSupport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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

@SpringBootTest(properties = "gestionale.security.session-touch-interval-seconds=1")
@Tag("postgresql")
class StableSessionSubjectTest extends PostgreSqlIntegrationTestSupport {
    @Autowired
    private UserService userService;

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    private AuthSessionRepository sessionRepository;

    @Autowired
    private UserAccountRepository accountRepository;

    @Test
    void persistsImmutableAccountSubjectAndCredentialVersion() {
        String username = unique("stable_subject");
        UserResponse user = userService.registerPublic(username, "Client123!");

        AuthSessionResponse issued = login(username, "Client123!", UserRole.CUSTOMER);
        AuthSession stored = sessions(user.id()).get(0);

        assertThat(stored.getAccountId()).isEqualTo(user.id());
        assertThat(stored.getUsername()).isEqualTo(username);
        assertThat(stored.getCredentialVersion()).isEqualTo(1L);
        assertThat(authSessionService.require(issued.token()).accountId()).isEqualTo(user.id());
    }

    @Test
    void disabledAccountRevokesTokensAndReservesUsernamePermanently() {
        String username = unique("recreated");
        UserResponse original = userService.registerPublic(username, "Client123!");
        AuthSessionResponse oldSession = login(username, "Client123!", UserRole.CUSTOMER);

        userService.delete(username, "test_super_admin");
        assertThatThrownBy(() -> authSessionService.require(oldSession.token()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Sessione non valida");
        assertThatThrownBy(() -> userService.login(username, "Client123!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credenziali non valide");
        assertThatThrownBy(() -> userService.registerPublic(username, "Different123!"))
                .hasMessageContaining("Esiste gia un account");
        assertThat(accountRepository.findById(original.id())).get().extracting(UserAccount::isEnabled).isEqualTo(false);
    }

    @Test
    void passwordChangeIncrementsCredentialVersionAndRevokesEverySession() {
        String username = unique("password_change");
        UserResponse user = userService.registerPublic(username, "Client123!");
        AuthSessionResponse first = login(username, "Client123!", UserRole.CUSTOMER);
        AuthSessionResponse second = login(username, "Client123!", UserRole.CUSTOMER);

        userService.changeOwnPassword(username, "Client123!", "Replacement123!");

        UserAccount account = accountRepository.findById(user.id()).orElseThrow();
        assertThat(account.getCredentialVersion()).isEqualTo(2L);
        assertThat(sessions(user.id())).allMatch(AuthSession::isRevoked);
        assertThatThrownBy(() -> authSessionService.require(first.token())).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> authSessionService.require(second.token())).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> userService.login(username, "Client123!"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(login(username, "Replacement123!", UserRole.CUSTOMER).token()).isNotBlank();
    }

    @Test
    void roleChangeIncrementsCredentialVersionAndRevokesEverySession() {
        String username = unique("role_change");
        UserResponse user = userService.createAccount(
                username,
                "Employee123!",
                UserRole.EMPLOYEE,
                "test_super_admin",
                "Creazione da pannello admin"
        );
        AuthSessionResponse session = login(username, "Employee123!", UserRole.EMPLOYEE);

        userService.changeRole(username, UserRole.CUSTOMER, "test_super_admin");

        UserAccount account = accountRepository.findById(user.id()).orElseThrow();
        assertThat(account.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(account.getCredentialVersion()).isEqualTo(2L);
        assertThat(sessions(user.id())).allMatch(AuthSession::isRevoked);
        assertThatThrownBy(() -> authSessionService.require(session.token()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void logoutAlwaysWinsAgainstConcurrentTouch() throws Exception {
        String username = unique("logout_touch");
        UserResponse user = userService.registerPublic(username, "Client123!");
        AuthSessionResponse session = login(username, "Client123!", UserRole.CUSTOMER);
        ageLastUse(user.id());

        runConcurrently(
                () -> ignoreUnauthorized(() -> authSessionService.require(session.token())),
                () -> authSessionService.logout(session.token())
        );

        assertThat(sessions(user.id())).singleElement().matches(AuthSession::isRevoked);
        assertThatThrownBy(() -> authSessionService.require(session.token()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rotationAlwaysWinsAgainstConcurrentTouchAndOldTokenCannotReturn() throws Exception {
        String username = unique("rotate_touch");
        UserResponse user = userService.registerPublic(username, "Client123!");
        AuthSessionResponse original = login(username, "Client123!", UserRole.CUSTOMER);
        ageLastUse(user.id());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            Future<?> touch = executor.submit(() -> awaitAndRun(
                    ready,
                    start,
                    () -> ignoreUnauthorized(() -> authSessionService.require(original.token()))
            ));
            Future<AuthSessionResponse> rotate = executor.submit(() -> {
                ready.countDown();
                await(start);
                return authSessionService.rotate(original.token());
            });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            touch.get(10, TimeUnit.SECONDS);
            AuthSessionResponse replacement = rotate.get(10, TimeUnit.SECONDS);

            assertThatThrownBy(() -> authSessionService.require(original.token()))
                    .isInstanceOf(UnauthorizedException.class);
            assertThat(authSessionService.require(replacement.token()).accountId()).isEqualTo(user.id());
            assertThat(sessions(user.id())).filteredOn(session -> !session.isRevoked()).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private AuthSessionResponse login(String username, String password, UserRole role) {
        return authSessionService.create(userService.login(username, password));
    }

    private List<AuthSession> sessions(Long accountId) {
        return sessionRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    private void ageLastUse(Long accountId) {
        AuthSession session = sessions(accountId).get(0);
        session.touch(Instant.now().minusSeconds(5));
        sessionRepository.saveAndFlush(session);
    }

    private void runConcurrently(Runnable first, Runnable second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            Future<?> firstResult = executor.submit(() -> awaitAndRun(ready, start, first));
            Future<?> secondResult = executor.submit(() -> awaitAndRun(ready, start, second));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            firstResult.get(10, TimeUnit.SECONDS);
            secondResult.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    private void awaitAndRun(CountDownLatch ready, CountDownLatch start, Runnable action) {
        ready.countDown();
        await(start);
        action.run();
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timeout sincronizzazione test.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Test interrotto.", exception);
        }
    }

    private void ignoreUnauthorized(Runnable action) {
        try {
            action.run();
        } catch (UnauthorizedException ignored) {
        }
    }

    private String unique(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
