package it.giovannidefilippo.gestionale.audit;

import java.time.LocalDateTime;

public record AuditEventResponse(
        Long id,
        LocalDateTime timestamp,
        String actor,
        String role,
        String action,
        String target,
        String details,
        String requestId,
        String source,
        String entityType,
        AuditCategory category,
        AuditSeverity severity
) {
    static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getTimestamp(),
                event.getActor(),
                event.getRole(),
                event.getAction(),
                event.getTarget(),
                event.getDetails(),
                event.getRequestId(),
                event.getSource(),
                event.getEntityType(),
                event.getCategory(),
                event.getSeverity()
        );
    }
}
