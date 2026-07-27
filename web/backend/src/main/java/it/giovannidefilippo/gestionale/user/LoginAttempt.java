package it.giovannidefilippo.gestionale.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "login_attempts")
public class LoginAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String usernameKey;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant firstAttemptAt;

    @Column(nullable = false)
    private Instant lastAttemptAt;

    private Instant lockedUntil;

    protected LoginAttempt() {
    }

    LoginAttempt(String usernameKey, Instant now) {
        this.usernameKey = usernameKey;
        this.firstAttemptAt = now;
        this.lastAttemptAt = now;
    }

    void registerFailure(Instant now, int maxAttempts, Duration lockDuration) {
        if (lockedUntil != null && lockedUntil.isBefore(now)) {
            attempts = 0;
            lockedUntil = null;
            firstAttemptAt = now;
        }
        attempts++;
        lastAttemptAt = now;
        if (attempts >= maxAttempts) {
            lockedUntil = now.plus(lockDuration);
        }
    }

    boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    Long getId() {
        return id;
    }

    String getUsernameKey() {
        return usernameKey;
    }

    int getAttempts() {
        return attempts;
    }

    Instant getFirstAttemptAt() {
        return firstAttemptAt;
    }

    Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    Instant getLockedUntil() {
        return lockedUntil;
    }
}
