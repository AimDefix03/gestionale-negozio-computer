package it.giovannidefilippo.gestionale.audit;

import it.giovannidefilippo.gestionale.common.CorrelationId;
import it.giovannidefilippo.gestionale.common.PageRequests;
import it.giovannidefilippo.gestionale.common.PageResponse;
import it.giovannidefilippo.gestionale.common.TimeProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class AuditService {
    private final AuditEventRepository repository;
    private final TimeProvider timeProvider;

    AuditService(AuditEventRepository repository, TimeProvider timeProvider) {
        this.repository = repository;
        this.timeProvider = timeProvider;
    }

    @Transactional
    public void record(String actor, String role, String action, String target, String details) {
        record(actor, role, action, target, details, AuditCategory.SYSTEM, AuditSeverity.INFO, "SYSTEM");
    }

    @Transactional
    public void record(String actor, String role, String action, String target, String details, AuditCategory category, AuditSeverity severity) {
        record(actor, role, action, target, details, category, severity, category == null ? "SYSTEM" : category.name());
    }

    @Transactional
    public void record(String actor, String role, String action, String target, String details, AuditCategory category, AuditSeverity severity, String entityType) {
        AuditRequestContext context = currentRequestContext();
        repository.save(new AuditEvent(actor, role, action, target, details, category, severity, context.requestId(), context.source(), entityType, timeProvider.localDateTime()));
    }

    public List<AuditEventResponse> findAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(AuditEvent::getTimestamp).reversed())
                .map(AuditEventResponse::from)
                .toList();
    }

    public PageResponse<AuditEventResponse> search(String q, AuditCategory category, AuditSeverity severity, int page, int size) {
        return PageResponse.from(repository.findAll(specification(q, category, severity), PageRequests.of(page, size, Sort.by("timestamp").descending()))
                .map(AuditEventResponse::from));
    }

    public List<AuditEventResponse> recentImportantEvents(int limit) {
        return repository.findBySeverityIn(List.of(AuditSeverity.CRITICAL, AuditSeverity.WARNING), PageRequest.of(0, limit, Sort.by("timestamp").descending()))
                .stream()
                .map(AuditEventResponse::from)
                .toList();
    }

    public long countBySeveritySince(AuditSeverity severity, LocalDateTime timestamp) {
        return repository.countBySeverityAndTimestampAfter(severity, timestamp);
    }

    private Specification<AuditEvent> specification(String q, AuditCategory category, AuditSeverity severity) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(q)) {
                String term = contains(q);
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("actor")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("role")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("action")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("target")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("details")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("requestId")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("source")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("entityType")), term)
                ));
            }
            if (category != null) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }
            if (severity != null) {
                predicates.add(criteriaBuilder.equal(root.get("severity"), severity));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String contains(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private AuditRequestContext currentRequestContext() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return new AuditRequestContext(CorrelationId.from(request), request.getMethod() + " " + request.getRequestURI());
        }
        return new AuditRequestContext("-", "SYSTEM");
    }

    private record AuditRequestContext(String requestId, String source) {
    }
}
