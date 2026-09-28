package it.giovannidefilippo.gestionale.idempotency;

public class IdempotencyInProgressException extends RuntimeException {
    public IdempotencyInProgressException(String message) {
        super(message);
    }
}
