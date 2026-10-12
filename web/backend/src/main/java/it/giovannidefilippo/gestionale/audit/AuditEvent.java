package it.giovannidefilippo.gestionale.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String actor;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String target;

    @Column(nullable = false, length = 1200)
    private String details;

    @Column(nullable = false, length = 80)
    private String requestId;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false, length = 80)
    private String entityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditSeverity severity;

    protected AuditEvent() {
    }

    AuditEvent(String actor, String role, String action, String target, String details, AuditCategory category, AuditSeverity severity, String requestId, String source, String entityType, LocalDateTime timestamp) {
        this.timestamp = timestamp;
        this.actor = clean(actor);
        this.role = clean(role);
        this.action = clean(action);
        this.target = clean(target);
        this.details = clean(details);
        this.requestId = clean(requestId);
        this.source = clean(source);
        this.entityType = clean(entityType);
        this.category = category == null ? AuditCategory.SYSTEM : category;
        this.severity = severity == null ? AuditSeverity.INFO : severity;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getActor() {
        return actor;
    }

    public String getRole() {
        return role;
    }

    public String getAction() {
        return action;
    }

    public String getTarget() {
        return target;
    }

    public String getDetails() {
        return details;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getSource() {
        return source;
    }

    public String getEntityType() {
        return entityType;
    }

    public AuditCategory getCategory() {
        return category;
    }

    public AuditSeverity getSeverity() {
        return severity;
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}
