package it.giovannidefilippo.gestionale.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.giovannidefilippo.gestionale.common.TimeProvider;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Supplier;

@Service
public class IdempotencyService {
    public static final String HEADER_NAME = "Idempotency-Key";

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;
    private final TimeProvider timeProvider;

    IdempotencyService(IdempotencyRecordRepository repository, ObjectMapper objectMapper, TimeProvider timeProvider) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.timeProvider = timeProvider;
    }

    @Transactional
    public <T> T execute(String rawKey, AuthenticatedUser actor, String operation, Object requestPayload, Class<T> responseType, HttpStatus httpStatus, Supplier<T> action) {
        if (!StringUtils.hasText(rawKey)) {
            return action.get();
        }

        String key = normalize(rawKey);
        String requestHash = hash(operation, requestPayload);
        IdempotencyRecord existing = repository.findByActorAndOperationAndIdempotencyKey(actor.username(), operation, key).orElse(null);
        if (existing != null) {
            return replay(existing, requestHash, responseType);
        }

        IdempotencyRecord record = repository.saveAndFlush(IdempotencyRecord.processing(key, actor.username(), operation, requestHash, timeProvider.localDateTime()));
        T response = action.get();
        record.complete(serialize(response), responseType.getName(), httpStatus.value(), timeProvider.localDateTime());
        return response;
    }

    private <T> T replay(IdempotencyRecord record, String requestHash, Class<T> responseType) {
        if (!record.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException("La stessa Idempotency-Key è già stata usata con dati diversi.");
        }
        if (record.getStatus() != IdempotencyStatus.COMPLETED || !StringUtils.hasText(record.getResponseBody())) {
            throw new IdempotencyConflictException("Operazione già in corso con la stessa Idempotency-Key.");
        }
        try {
            return objectMapper.readValue(record.getResponseBody(), responseType);
        } catch (JsonProcessingException exception) {
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

    private String hash(String operation, Object requestPayload) {
        try {
            String payload = objectMapper.writeValueAsString(requestPayload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((operation + "\n" + payload).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Impossibile calcolare l'impronta della richiesta.", exception);
        }
    }

    private String serialize(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Impossibile salvare la risposta idempotente.", exception);
        }
    }
}
