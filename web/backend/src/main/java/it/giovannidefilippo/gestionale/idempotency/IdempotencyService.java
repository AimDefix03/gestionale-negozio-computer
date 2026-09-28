package it.giovannidefilippo.gestionale.idempotency;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class IdempotencyService {
    public static final String HEADER_NAME = "Idempotency-Key";
    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyClaimService claimService;
    private final IdempotencyWorkService workService;
    private final ObjectMapper objectMapper;
    private final Duration waitTimeout;
    private final Duration pollInterval;

    IdempotencyService(
            IdempotencyClaimService claimService,
            IdempotencyWorkService workService,
            ObjectMapper objectMapper,
            @Value("${gestionale.idempotency.wait-timeout-ms:5000}") long waitTimeoutMs,
            @Value("${gestionale.idempotency.poll-interval-ms:25}") long pollIntervalMs
    ) {
        this.claimService = claimService;
        this.workService = workService;
        this.objectMapper = objectMapper;
        this.waitTimeout = Duration.ofMillis(waitTimeoutMs);
        this.pollInterval = Duration.ofMillis(pollIntervalMs);
    }

    public <T> T execute(
            String rawKey,
            AuthenticatedUser actor,
            String operation,
            Object requestPayload,
            Class<T> responseType,
            HttpStatus httpStatus,
            Supplier<T> action
    ) {
        if (!StringUtils.hasText(rawKey)) {
            return action.get();
        }
        if (actor.accountId() == null) {
            throw new IllegalStateException("La sessione non dispone di un identificativo account stabile.");
        }

        String key = normalize(rawKey);
        String payload = serializePayload(requestPayload);
        String requestHash = hash("v2\nactor:" + actor.accountId() + "\nendpoint:" + operation + "\npayload:" + payload);
        String legacyRequestHash = hash(operation + "\n" + payload);
        long deadline = System.nanoTime() + waitTimeout.toNanos();
        String claimToken = newClaimToken();
        IdempotencyClaimResult claim = createOrResolve(
                key,
                actor,
                operation,
                requestHash,
                legacyRequestHash,
                claimToken
        );

        while (true) {
            if (claim.decision() == IdempotencyClaimResult.Decision.ACQUIRED) {
                return executeClaim(key, actor, operation, claim.claimToken(), responseType, httpStatus, action);
            }
            if (claim.decision() == IdempotencyClaimResult.Decision.REPLAY) {
                return deserialize(claim.responseBody(), responseType);
            }
            if (System.nanoTime() >= deadline) {
                throw new IdempotencyInProgressException("Operazione ancora in corso. Riprova con la stessa Idempotency-Key.");
            }
            pause();
            claimToken = newClaimToken();
            claim = inspectOrReclaim(
                    key,
                    actor,
                    operation,
                    requestHash,
                    legacyRequestHash,
                    claimToken
            );
        }
    }

    private IdempotencyClaimResult createOrResolve(
            String key,
            AuthenticatedUser actor,
            String operation,
            String requestHash,
            String legacyRequestHash,
            String claimToken
    ) {
        IdempotencyClaimResult existing = inspectOrReclaim(
                key,
                actor,
                operation,
                requestHash,
                legacyRequestHash,
                claimToken
        );
        if (existing.decision() != IdempotencyClaimResult.Decision.MISSING) {
            return existing;
        }
        try {
            return claimService.create(key, actor.accountId(), actor.username(), operation, requestHash, claimToken);
        } catch (DataIntegrityViolationException exception) {
            return inspectOrReclaim(
                    key,
                    actor,
                    operation,
                    requestHash,
                    legacyRequestHash,
                    claimToken
            );
        }
    }

    private IdempotencyClaimResult inspectOrReclaim(
            String key,
            AuthenticatedUser actor,
            String operation,
            String requestHash,
            String legacyRequestHash,
            String claimToken
    ) {
        IdempotencyClaimResult inspected = claimService.inspect(
                key,
                actor.accountId(),
                operation,
                requestHash,
                legacyRequestHash
        );
        if (inspected.decision() != IdempotencyClaimResult.Decision.RECLAIMABLE) {
            return inspected;
        }
        return claimService.reclaim(
                key,
                actor.accountId(),
                actor.username(),
                operation,
                requestHash,
                legacyRequestHash,
                claimToken
        );
    }

    private <T> T executeClaim(
            String key,
            AuthenticatedUser actor,
            String operation,
            String claimToken,
            Class<T> responseType,
            HttpStatus httpStatus,
            Supplier<T> action
    ) {
        try {
            return workService.execute(
                    key,
                    actor.accountId(),
                    operation,
                    claimToken,
                    responseType,
                    httpStatus.value(),
                    action
            );
        } catch (RuntimeException exception) {
            try {
                claimService.markFailedRetryable(key, actor.accountId(), operation, claimToken);
            } catch (RuntimeException markerFailure) {
                log.error("Impossibile marcare come ritentabile il claim idempotente per {}", operation, markerFailure);
            }
            throw exception;
        }
    }

    private <T> T deserialize(String responseBody, Class<T> responseType) {
        if (!StringUtils.hasText(responseBody)) {
            throw new IllegalStateException("Risposta idempotente non disponibile.");
        }
        try {
            return objectMapper.readValue(responseBody, responseType);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Risposta idempotente non leggibile.", exception);
        }
    }

    private String normalize(String rawKey) {
        String key = rawKey.trim();
        if (key.length() > 120) {
            throw new IllegalArgumentException("Idempotency-Key troppo lunga.");
        }
        return key;
    }

    private String serializePayload(Object requestPayload) {
        try {
            return objectMapper.writeValueAsString(requestPayload);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Impossibile serializzare la richiesta idempotente.", exception);
        }
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 non disponibile.", exception);
        }
    }

    private String newClaimToken() {
        return UUID.randomUUID().toString();
    }

    private void pause() {
        try {
            Thread.sleep(pollInterval.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IdempotencyInProgressException("Attesa del risultato idempotente interrotta. Riprova con la stessa Idempotency-Key.");
        }
    }
}
