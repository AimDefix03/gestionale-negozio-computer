package it.giovannidefilippo.gestionale.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "auth_sessions")
public class AuthSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant lastUsedAt;

    private Instant revokedAt;

    protected AuthSession() {
    }

    AuthSession(String tokenHash, String username, UserRole role, Instant createdAt, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.username = username;
        this.role = role;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.lastUsedAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getUsername() {
        return username;
    }

    public UserRole getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isIdleExpired(Instant now, Duration idleTimeout) {
        return !lastUsedAt.plus(idleTimeout).isAfter(now);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public void revoke(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public void touch(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
