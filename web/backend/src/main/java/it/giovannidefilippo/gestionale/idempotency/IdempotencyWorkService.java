package it.giovannidefilippo.gestionale.idempotency;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import it.giovannidefilippo.gestionale.common.TimeProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Supplier;

@Service
class IdempotencyWorkService {
    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;
    private final TimeProvider timeProvider;
    private final Duration retentionDuration;

    IdempotencyWorkService(
            IdempotencyRecordRepository repository,
            ObjectMapper objectMapper,
            TimeProvider timeProvider,
            @Value("${gestionale.idempotency.retention-hours:24}") long retentionHours
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.timeProvider = timeProvider;
        this.retentionDuration = Duration.ofHours(retentionHours);
    }

    @Transactional
    public <T> T execute(
            String key,
            Long actorAccountId,
            String operation,
            String claimToken,
            Class<T> responseType,
            int httpStatus,
            Supplier<T> action
    ) {
        IdempotencyRecord record = repository.findLocked(actorAccountId, operation, key)
                .orElseThrow(() -> new IllegalStateException("Claim idempotente non trovato."));
        if (record.getStatus() != IdempotencyStatus.IN_PROGRESS || !claimToken.equals(record.getClaimToken())) {
            throw new IllegalStateException("Claim idempotente non piu valido.");
        }

        T response = action.get();
        LocalDateTime now = timeProvider.localDateTime();
        record.complete(serialize(response), responseType.getName(), httpStatus, now, now.plus(retentionDuration));
        return response;
    }

    private String serialize(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Impossibile salvare la risposta idempotente.", exception);
        }
    }
}
