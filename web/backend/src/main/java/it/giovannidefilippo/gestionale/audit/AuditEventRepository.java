package it.giovannidefilippo.gestionale.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

interface AuditEventRepository extends JpaRepository<AuditEvent, Long>, JpaSpecificationExecutor<AuditEvent> {
    List<AuditEvent> findBySeverityIn(Collection<AuditSeverity> severities, Pageable pageable);

    long countBySeverityAndTimestampAfter(AuditSeverity severity, LocalDateTime timestamp);
}
