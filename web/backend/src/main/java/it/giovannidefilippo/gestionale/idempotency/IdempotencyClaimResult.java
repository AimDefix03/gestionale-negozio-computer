package it.giovannidefilippo.gestionale.idempotency;

record IdempotencyClaimResult(Decision decision, String claimToken, String responseBody, Integer httpStatus) {
    enum Decision {
        ACQUIRED,
        REPLAY,
        IN_PROGRESS,
        RECLAIMABLE,
        MISSING
    }

    static IdempotencyClaimResult acquired(String claimToken) {
        return new IdempotencyClaimResult(Decision.ACQUIRED, claimToken, null, null);
    }

    static IdempotencyClaimResult replay(String responseBody, Integer httpStatus) {
        return new IdempotencyClaimResult(Decision.REPLAY, null, responseBody, httpStatus);
    }

    static IdempotencyClaimResult inProgress() {
        return new IdempotencyClaimResult(Decision.IN_PROGRESS, null, null, null);
    }

    static IdempotencyClaimResult reclaimable() {
        return new IdempotencyClaimResult(Decision.RECLAIMABLE, null, null, null);
    }

    static IdempotencyClaimResult missing() {
        return new IdempotencyClaimResult(Decision.MISSING, null, null, null);
    }
}
