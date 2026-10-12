package it.giovannidefilippo.gestionale.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface PhysicalInventorySessionRepository extends JpaRepository<PhysicalInventorySession, Long>, JpaSpecificationExecutor<PhysicalInventorySession> {
    @EntityGraph(attributePaths = "items")
    Optional<PhysicalInventorySession> findDetailedById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from PhysicalInventorySession session where session.id = :id")
    Optional<PhysicalInventorySession> findByIdForUpdate(@Param("id") Long id);
}
