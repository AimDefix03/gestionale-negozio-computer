package it.giovannidefilippo.gestionale.user;

import it.giovannidefilippo.gestionale.common.TimeProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
class SecurityMaintenanceService {
    private final AuthSessionRepository sessionRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final TimeProvider timeProvider;
    private final long sessionRetentionDays;
    private final long loginAttemptRetentionHours;

    SecurityMaintenanceService(
            AuthSessionRepository sessionRepository,
            LoginAttemptRepository loginAttemptRepository,
            TimeProvider timeProvider,
            @Value("${gestionale.security.session-retention-days:7}") long sessionRetentionDays,
            @Value("${gestionale.security.login-attempt-retention-hours:24}") long loginAttemptRetentionHours
    ) {
        this.sessionRepository = sessionRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.timeProvider = timeProvider;
        this.sessionRetentionDays = sessionRetentionDays;
        this.loginAttemptRetentionHours = loginAttemptRetentionHours;
    }

    @Scheduled(
            fixedDelayString = "${gestionale.security.cleanup-delay-ms:900000}",
            initialDelayString = "${gestionale.security.cleanup-initial-delay-ms:900000}"
    )
    @Transactional
    void cleanup() {
        Instant now = timeProvider.instant();
        sessionRepository.deleteExpiredOrRevokedBefore(now.minus(Duration.ofDays(sessionRetentionDays)));
        loginAttemptRepository.deleteExpiredLocksBefore(now);
        loginAttemptRepository.deleteOldUnlockedAttempts(now.minus(Duration.ofHours(loginAttemptRetentionHours)));
    }
}
