package it.giovannidefilippo.gestionale.user;

import it.giovannidefilippo.gestionale.audit.AuditCategory;
import it.giovannidefilippo.gestionale.audit.AuditService;
import it.giovannidefilippo.gestionale.audit.AuditSeverity;
import it.giovannidefilippo.gestionale.common.ForbiddenException;
import it.giovannidefilippo.gestionale.common.TimeProvider;
import it.giovannidefilippo.gestionale.common.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
@Transactional(readOnly = true)
public class AuthSessionService {
    private static final Duration LOGIN_LOCK_DURATION = Duration.ofMinutes(5);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int MAX_TOKEN_LENGTH = 256;

    private final SecureRandom secureRandom = new SecureRandom();
    private final AuthSessionRepository sessionRepository;
    private final UserAccountRepository accountRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final TimeProvider timeProvider;
    private final AuditService auditService;
    private final Duration sessionDuration;
    private final Duration idleTimeout;
    private final Duration touchInterval;

    AuthSessionService(
            AuthSessionRepository sessionRepository,
            UserAccountRepository accountRepository,
            LoginAttemptRepository loginAttemptRepository,
            TimeProvider timeProvider,
            AuditService auditService,
            @Value("${gestionale.security.session-duration-minutes:45}") long sessionDurationMinutes,
            @Value("${gestionale.security.session-idle-timeout-minutes:30}") long idleTimeoutMinutes,
            @Value("${gestionale.security.session-touch-interval-seconds:60}") long touchIntervalSeconds
    ) {
        this.sessionRepository = sessionRepository;
        this.accountRepository = accountRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.timeProvider = timeProvider;
        this.auditService = auditService;
        this.sessionDuration = positiveDuration(sessionDurationMinutes, Duration.ofMinutes(1), "durata sessione");
        this.idleTimeout = positiveDuration(idleTimeoutMinutes, Duration.ofMinutes(1), "timeout inattivita");
        this.touchInterval = positiveDuration(touchIntervalSeconds, Duration.ofSeconds(1), "intervallo aggiornamento sessione");
        if (idleTimeout.compareTo(sessionDuration) > 0) {
            throw new IllegalArgumentException("Il timeout di inattivita non puo superare la durata della sessione.");
        }
    }

    @Transactional
    public AuthSessionResponse create(UserResponse user) {
        return issue(user, timeProvider.instant());
    }

    @Transactional
    public AuthSessionResponse rotate(String token) {
        String normalizedToken = normalizeToken(token);
        Instant now = timeProvider.instant();
        AuthSession session = sessionRepository.findByTokenHashForUpdate(tokenHash(normalizedToken))
                .orElseThrow(() -> new UnauthorizedException("Sessione non valida. Effettua di nuovo il login."));
        UserAccount account = validate(session, now);
        session.revoke(now);
        AuthSessionResponse response = issue(UserResponse.from(account), now);
        auditService.record(account.getUsername(), account.getRole().getLabel(), "RENEW_SESSION", account.getUsername(), "Sessione rinnovata con rotazione del token", AuditCategory.SECURITY, AuditSeverity.INFO, "SESSION");
        return response;
    }

    private AuthSessionResponse issue(UserResponse user, Instant now) {
        String token = token();
        Instant expiresAt = now.plus(sessionDuration);
        sessionRepository.save(new AuthSession(tokenHash(token), user.username(), user.role(), now, expiresAt));
        return new AuthSessionResponse(user, token, expiresAt);
    }

    @Transactional
    public AuthenticatedUser require(String token) {
        String normalizedToken = normalizeToken(token);
        AuthSession session = sessionRepository.findByTokenHash(tokenHash(normalizedToken))
                .orElse(null);
        if (session == null) {
            throw new UnauthorizedException("Sessione non valida. Effettua di nuovo il login.");
        }
        Instant now = timeProvider.instant();
        UserAccount account = validate(session, now);
        if (!session.getLastUsedAt().plus(touchInterval).isAfter(now)) {
            session.touch(now);
        }
        return new AuthenticatedUser(account.getUsername(), account.getRole());
    }

    public AuthenticatedUser requireManageOperations(String token) {
        AuthenticatedUser user = require(token);
        if (!user.role().canManageOperations()) {
            throw new ForbiddenException("Permessi insufficienti per questa operazione.");
        }
        return user;
    }

    public AuthenticatedUser requireManageAccounts(String token) {
        return requirePermission(token, UserPermission.MANAGE_ACCOUNTS);
    }

    public AuthenticatedUser requirePermission(String token, UserPermission permission) {
        AuthenticatedUser user = require(token);
        if (!user.hasPermission(permission)) {
            throw new ForbiddenException("Permessi insufficienti per questa operazione.");
        }
        return user;
    }

    @Transactional
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            sessionRepository.findByTokenHash(tokenHash(token.trim()))
                    .filter(session -> !session.isRevoked())
                    .ifPresent(session -> session.revoke(timeProvider.instant()));
        }
    }

    @Transactional
    public void assertLoginAllowed(String username) {
        String usernameKey = key(username);
        LoginAttempt loginAttempt = loginAttemptRepository.findByUsernameKey(usernameKey).orElse(null);
        if (loginAttempt == null) {
            return;
        }
        Instant now = timeProvider.instant();
        if (loginAttempt.isLocked(now)) {
            throw new UnauthorizedException("Troppi tentativi non riusciti. Riprova tra qualche minuto.");
        }
        if (loginAttempt.getLockedUntil() != null) {
            loginAttemptRepository.delete(loginAttempt);
        }
    }

    @Transactional
    public void registerFailedLogin(String username) {
        String usernameKey = key(username);
        if (usernameKey.isBlank()) {
            return;
        }
        Instant now = timeProvider.instant();
        LoginAttempt loginAttempt = loginAttemptRepository.findByUsernameKey(usernameKey)
                .orElseGet(() -> new LoginAttempt(usernameKey, now));
        loginAttempt.registerFailure(now, MAX_FAILED_ATTEMPTS, LOGIN_LOCK_DURATION);
        loginAttemptRepository.save(loginAttempt);
    }

    @Transactional
    public void clearFailedLogin(String username) {
        loginAttemptRepository.deleteByUsernameKey(key(username));
    }

    private String token() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String key(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private String tokenHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Algoritmo SHA-256 non disponibile.", exception);
        }
    }

    private UserAccount validate(AuthSession session, Instant now) {
        if (session.isRevoked()) {
            throw new UnauthorizedException("Sessione non valida. Effettua di nuovo il login.");
        }
        if (session.isExpired(now)) {
            session.revoke(now);
            throw new UnauthorizedException("Sessione scaduta. Effettua di nuovo il login.");
        }
        if (session.isIdleExpired(now, idleTimeout)) {
            session.revoke(now);
            throw new UnauthorizedException("Sessione scaduta per inattivita. Effettua di nuovo il login.");
        }
        UserAccount account = accountRepository.findByUsernameIgnoreCase(session.getUsername()).orElse(null);
        if (account == null) {
            session.revoke(now);
            throw new UnauthorizedException("Account non piu disponibile. Effettua di nuovo il login.");
        }
        return account;
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("Sessione mancante. Effettua il login.");
        }
        String normalized = token.trim();
        if (normalized.length() > MAX_TOKEN_LENGTH) {
            throw new UnauthorizedException("Sessione non valida. Effettua di nuovo il login.");
        }
        return normalized;
    }

    private static Duration positiveDuration(long value, Duration unit, String label) {
        if (value <= 0) {
            throw new IllegalArgumentException("La configurazione " + label + " deve essere positiva.");
        }
        return unit.multipliedBy(value);
    }

}
