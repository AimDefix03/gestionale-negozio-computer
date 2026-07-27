package it.giovannidefilippo.gestionale.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    Optional<LoginAttempt> findByUsernameKey(String usernameKey);

    long deleteByUsernameKey(String usernameKey);

    @Modifying
    @Query("delete from LoginAttempt attempt where attempt.lockedUntil is not null and attempt.lockedUntil < :now")
    int deleteExpiredLocksBefore(Instant now);

    @Modifying
    @Query("delete from LoginAttempt attempt where attempt.lockedUntil is null and attempt.lastAttemptAt < :threshold")
    int deleteOldUnlockedAttempts(Instant threshold);

    @Query("select count(attempt) from LoginAttempt attempt where attempt.lockedUntil is not null and attempt.lockedUntil > :now")
    long countLockedAttempts(Instant now);

    @Query("select count(attempt) from LoginAttempt attempt where attempt.lastAttemptAt > :threshold")
    long countRecentAttempts(Instant threshold);
}
