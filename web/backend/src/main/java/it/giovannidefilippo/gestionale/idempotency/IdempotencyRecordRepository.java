package it.giovannidefilippo.gestionale.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {
    Optional<IdempotencyRecord> findByActorAndOperationAndIdempotencyKey(String actor, String operation, String idempotencyKey);
}
