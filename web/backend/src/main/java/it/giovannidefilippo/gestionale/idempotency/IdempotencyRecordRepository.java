package it.giovannidefilippo.gestionale.idempotency;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {
    Optional<IdempotencyRecord> findByActorAccountIdAndOperationAndIdempotencyKey(
            Long actorAccountId,
            String operation,
            String idempotencyKey
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select record from IdempotencyRecord record
            where record.actorAccountId = :actorAccountId
              and record.operation = :operation
              and record.idempotencyKey = :idempotencyKey
            """)
    Optional<IdempotencyRecord> findLocked(
            @Param("actorAccountId") Long actorAccountId,
            @Param("operation") String operation,
            @Param("idempotencyKey") String idempotencyKey
    );

    @Modifying
    @Query(value = """
            delete from idempotency_records
            where expires_at < :now
              and (status <> 'IN_PROGRESS' or lease_expires_at is null or lease_expires_at < :now)
            """, nativeQuery = true)
    int deleteExpired(@Param("now") LocalDateTime now);
}
