package it.giovannidefilippo.gestionale.user;

import it.giovannidefilippo.gestionale.common.TimeProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class SecurityMetricsService {
    private final AuthSessionRepository sessionRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final TimeProvider timeProvider;
    private final Duration idleTimeout;

    SecurityMetricsService(
            AuthSessionRepository sessionRepository,
            LoginAttemptRepository loginAttemptRepository,
            TimeProvider timeProvider,
            @Value("${gestionale.security.session-idle-timeout-minutes:30}") long idleTimeoutMinutes
    ) {
        this.sessionRepository = sessionRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.timeProvider = timeProvider;
        if (idleTimeoutMinutes <= 0) {
            throw new IllegalArgumentException("Il timeout di inattivita deve essere positivo.");
        }
        this.idleTimeout = Duration.ofMinutes(idleTimeoutMinutes);
    }

    public SecurityMetrics snapshot() {
        Instant now = timeProvider.instant();
        return new SecurityMetrics(
                sessionRepository.countActiveSessions(now, now.minus(idleTimeout)),
                loginAttemptRepository.countLockedAttempts(now),
                loginAttemptRepository.countRecentAttempts(now.minusSeconds(3600))
        );
    }

    public record SecurityMetrics(
            long activeSessions,
            long lockedLoginAttempts,
            long recentLoginAttempts
    ) {
    }
}
