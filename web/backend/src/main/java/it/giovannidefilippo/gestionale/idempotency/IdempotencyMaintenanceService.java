package it.giovannidefilippo.gestionale.idempotency;

import it.giovannidefilippo.gestionale.common.TimeProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class IdempotencyMaintenanceService {
    private final IdempotencyRecordRepository repository;
    private final TimeProvider timeProvider;

    IdempotencyMaintenanceService(IdempotencyRecordRepository repository, TimeProvider timeProvider) {
        this.repository = repository;
        this.timeProvider = timeProvider;
    }

    @Scheduled(
            fixedDelayString = "${gestionale.idempotency.cleanup-delay-ms:900000}",
            initialDelayString = "${gestionale.idempotency.cleanup-initial-delay-ms:900000}"
    )
    @Transactional
    void cleanup() {
        repository.deleteExpired(timeProvider.localDateTime());
    }
}
