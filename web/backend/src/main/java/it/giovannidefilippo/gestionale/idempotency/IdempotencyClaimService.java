package it.giovannidefilippo.gestionale.idempotency;

import it.giovannidefilippo.gestionale.common.TimeProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
class IdempotencyClaimService {
    private final IdempotencyRecordRepository repository;
    private final TimeProvider timeProvider;
    private final Duration leaseDuration;
    private final Duration retentionDuration;

    IdempotencyClaimService(
            IdempotencyRecordRepository repository,
            TimeProvider timeProvider,
            @Value("${gestionale.idempotency.lease-seconds:300}") long leaseSeconds,
            @Value("${gestionale.idempotency.retention-hours:24}") long retentionHours
    ) {
        this.repository = repository;
        this.timeProvider = timeProvider;
        this.leaseDuration = Duration.ofSeconds(leaseSeconds);
        this.retentionDuration = Duration.ofHours(retentionHours);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyClaimResult create(
            String key,
            Long actorAccountId,
            String actor,
            String operation,
            String requestHash,
            String claimToken
    ) {
        LocalDateTime now = timeProvider.localDateTime();
        repository.saveAndFlush(IdempotencyRecord.inProgress(
                key,
                actorAccountId,
                actor,
                operation,
                requestHash,
                claimToken,
                now,
                now.plus(leaseDuration),
                now.plus(retentionDuration)
        ));
        return IdempotencyClaimResult.acquired(claimToken);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyClaimResult inspect(
            String key,
            Long actorAccountId,
            String operation,
            String requestHash,
            String legacyRequestHash
    ) {
        IdempotencyRecord record = repository.findByActorAccountIdAndOperationAndIdempotencyKey(actorAccountId, operation, key)
                .orElse(null);
        if (record == null) {
            return IdempotencyClaimResult.missing();
        }
        return evaluate(record, requestHash, legacyRequestHash, timeProvider.localDateTime());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyClaimResult reclaim(
            String key,
            Long actorAccountId,
            String actor,
            String operation,
            String requestHash,
            String legacyRequestHash,
            String nextClaimToken
    ) {
        IdempotencyRecord record = repository.findLocked(actorAccountId, operation, key).orElse(null);
        if (record == null) {
            return IdempotencyClaimResult.missing();
        }
        LocalDateTime now = timeProvider.localDateTime();
        IdempotencyClaimResult current = evaluate(record, requestHash, legacyRequestHash, now);
        if (current.decision() != IdempotencyClaimResult.Decision.RECLAIMABLE) {
            return current;
        }

        record.reclaim(
                actor,
                requestHash,
                nextClaimToken,
                now,
                now.plus(leaseDuration),
                now.plus(retentionDuration)
        );
        return IdempotencyClaimResult.acquired(nextClaimToken);
    }

    private IdempotencyClaimResult evaluate(
            IdempotencyRecord record,
            String requestHash,
            String legacyRequestHash,
            LocalDateTime now
    ) {
        String expectedHash = record.getFingerprintVersion() == 1 ? legacyRequestHash : requestHash;
        if (!record.getRequestHash().equals(expectedHash)) {
            throw new IdempotencyConflictException("La stessa Idempotency-Key e gia stata usata con dati diversi.");
        }

        boolean expired = !record.getExpiresAt().isAfter(now);
        if (record.getStatus() == IdempotencyStatus.COMPLETED && !expired) {
            return IdempotencyClaimResult.replay(record.getResponseBody(), record.getHttpStatus());
        }
        if (record.getStatus() == IdempotencyStatus.IN_PROGRESS
                && !expired
                && record.getLeaseExpiresAt() != null
                && record.getLeaseExpiresAt().isAfter(now)) {
            return IdempotencyClaimResult.inProgress();
        }
        return IdempotencyClaimResult.reclaimable();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailedRetryable(
            String key,
            Long actorAccountId,
            String operation,
            String claimToken
    ) {
        IdempotencyRecord record = repository.findLocked(actorAccountId, operation, key).orElse(null);
        if (record == null
                || record.getStatus() != IdempotencyStatus.IN_PROGRESS
                || !claimToken.equals(record.getClaimToken())) {
            return;
        }
        LocalDateTime now = timeProvider.localDateTime();
        record.failRetryable(now, now.plus(retentionDuration));
    }
}
