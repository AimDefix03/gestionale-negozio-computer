package it.giovannidefilippo.gestionale.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
class LoginAttemptWriteService {
    private final LoginAttemptRepository repository;

    LoginAttemptWriteService(LoginAttemptRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFailure(String usernameKey, Instant now, int maximumAttempts, Duration lockDuration) {
        LoginAttempt attempt = repository.findByUsernameKeyForUpdate(usernameKey)
                .orElseGet(() -> new LoginAttempt(usernameKey, now));
        attempt.registerFailure(now, maximumAttempts, lockDuration);
        repository.saveAndFlush(attempt);
    }
}
