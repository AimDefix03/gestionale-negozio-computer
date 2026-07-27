package it.giovannidefilippo.gestionale.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
    Optional<AuthSession> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from AuthSession session where session.tokenHash = :tokenHash")
    Optional<AuthSession> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    List<AuthSession> findByUsernameIgnoreCaseOrderByCreatedAtDesc(String username);

    @Modifying
    @Query("delete from AuthSession session where session.expiresAt < :threshold or (session.revokedAt is not null and session.revokedAt < :threshold)")
    int deleteExpiredOrRevokedBefore(Instant threshold);

    @Query("select count(session) from AuthSession session where session.revokedAt is null and session.expiresAt > :now and session.lastUsedAt > :idleThreshold")
    long countActiveSessions(@Param("now") Instant now, @Param("idleThreshold") Instant idleThreshold);
}
