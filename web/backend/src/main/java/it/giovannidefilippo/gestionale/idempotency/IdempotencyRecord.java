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
        uniqueConstraints = @UniqueConstraint(name = "uk_idempotency_actor_operation_key", columnNames = {"actor", "operation", "idem_key"})
)
class IdempotencyRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idem_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Column(nullable = false)
    private String actor;

    @Column(nullable = false)
    private String operation;

    @Column(nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IdempotencyStatus status;

    @Column(columnDefinition = "text")
    private String responseBody;

    @Column(length = 500)
    private String responseType;

    private Integer httpStatus;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    protected IdempotencyRecord() {
    }

    private IdempotencyRecord(String idempotencyKey, String actor, String operation, String requestHash, LocalDateTime createdAt) {
        this.idempotencyKey = idempotencyKey;
        this.actor = actor;
        this.operation = operation;
        this.requestHash = requestHash;
        this.status = IdempotencyStatus.PROCESSING;
        this.createdAt = createdAt;
    }

    static IdempotencyRecord processing(String idempotencyKey, String actor, String operation, String requestHash, LocalDateTime createdAt) {
        return new IdempotencyRecord(idempotencyKey, actor, operation, requestHash, createdAt);
    }

    String getRequestHash() {
        return requestHash;
    }

    IdempotencyStatus getStatus() {
        return status;
    }

    String getResponseBody() {
        return responseBody;
    }

    void complete(String responseBody, String responseType, int httpStatus, LocalDateTime completedAt) {
        this.responseBody = responseBody;
        this.responseType = responseType;
        this.httpStatus = httpStatus;
        this.status = IdempotencyStatus.COMPLETED;
        this.completedAt = completedAt;
    }
}
