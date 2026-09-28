package it.giovannidefilippo.gestionale.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "idempotency_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_idempotency_account_operation_key",
                columnNames = {"actor_account_id", "operation", "idem_key"}
        )
)
class IdempotencyRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idem_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Column(nullable = false)
    private String actor;

    @Column(name = "actor_account_id", nullable = false)
    private Long actorAccountId;

    @Column(nullable = false)
    private String operation;

    @Column(nullable = false, length = 64)
    private String requestHash;

    @Column(nullable = false)
    private int fingerprintVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IdempotencyStatus status;

    @Column(length = 64)
    private String claimToken;

    @Column(columnDefinition = "text")
    private String responseBody;

    @Column(length = 500)
    private String responseType;

    private Integer httpStatus;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

    private LocalDateTime failedAt;

    private LocalDateTime leaseExpiresAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    protected IdempotencyRecord() {
    }

    private IdempotencyRecord(
            String idempotencyKey,
            Long actorAccountId,
            String actor,
            String operation,
            String requestHash,
            String claimToken,
            LocalDateTime now,
            LocalDateTime leaseExpiresAt,
            LocalDateTime expiresAt
    ) {
        this.idempotencyKey = idempotencyKey;
        this.actorAccountId = actorAccountId;
        this.actor = actor;
        this.operation = operation;
        this.requestHash = requestHash;
        this.fingerprintVersion = 2;
        this.status = IdempotencyStatus.IN_PROGRESS;
        this.claimToken = claimToken;
        this.createdAt = now;
        this.updatedAt = now;
        this.leaseExpiresAt = leaseExpiresAt;
        this.expiresAt = expiresAt;
    }

    static IdempotencyRecord inProgress(
            String idempotencyKey,
            Long actorAccountId,
            String actor,
            String operation,
            String requestHash,
            String claimToken,
            LocalDateTime now,
            LocalDateTime leaseExpiresAt,
            LocalDateTime expiresAt
    ) {
        return new IdempotencyRecord(
                idempotencyKey,
                actorAccountId,
                actor,
                operation,
                requestHash,
                claimToken,
                now,
                leaseExpiresAt,
                expiresAt
        );
    }

    Long getId() {
        return id;
    }

    String getRequestHash() {
        return requestHash;
    }

    int getFingerprintVersion() {
        return fingerprintVersion;
    }

    IdempotencyStatus getStatus() {
        return status;
    }

    String getClaimToken() {
        return claimToken;
    }

    String getResponseBody() {
        return responseBody;
    }

    Integer getHttpStatus() {
        return httpStatus;
    }

    LocalDateTime getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    void reclaim(
            String actor,
            String requestHash,
            String claimToken,
            LocalDateTime now,
            LocalDateTime leaseExpiresAt,
            LocalDateTime expiresAt
    ) {
        this.actor = actor;
        this.requestHash = requestHash;
        this.fingerprintVersion = 2;
        this.status = IdempotencyStatus.IN_PROGRESS;
        this.claimToken = claimToken;
        this.responseBody = null;
        this.responseType = null;
        this.httpStatus = null;
        this.updatedAt = now;
        this.completedAt = null;
        this.failedAt = null;
        this.leaseExpiresAt = leaseExpiresAt;
        this.expiresAt = expiresAt;
    }

    void complete(String responseBody, String responseType, int httpStatus, LocalDateTime completedAt, LocalDateTime expiresAt) {
        this.responseBody = responseBody;
        this.responseType = responseType;
        this.httpStatus = httpStatus;
        this.status = IdempotencyStatus.COMPLETED;
        this.updatedAt = completedAt;
        this.completedAt = completedAt;
        this.failedAt = null;
        this.leaseExpiresAt = null;
        this.expiresAt = expiresAt;
    }

    void failRetryable(LocalDateTime failedAt, LocalDateTime expiresAt) {
        this.status = IdempotencyStatus.FAILED_RETRYABLE;
        this.updatedAt = failedAt;
        this.failedAt = failedAt;
        this.leaseExpiresAt = null;
        this.expiresAt = expiresAt;
    }
}
