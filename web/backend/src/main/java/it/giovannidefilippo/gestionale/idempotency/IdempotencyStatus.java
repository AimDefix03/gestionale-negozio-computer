package it.giovannidefilippo.gestionale.idempotency;

enum IdempotencyStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED_RETRYABLE
}
