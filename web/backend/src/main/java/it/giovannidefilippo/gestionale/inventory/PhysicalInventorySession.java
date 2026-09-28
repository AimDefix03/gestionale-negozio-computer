package it.giovannidefilippo.gestionale.inventory;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "physical_inventory_sessions")
class PhysicalInventorySession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PhysicalInventoryStatus status;

    @Column(nullable = false, length = 900)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String createdByRole;

    private LocalDateTime submittedAt;
    private String submittedBy;
    private String submittedByRole;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private String approvedByRole;

    @Column(length = 900)
    private String approvalReason;

    private LocalDateTime canceledAt;
    private String canceledBy;
    private String canceledByRole;

    @Column(length = 900)
    private String cancellationReason;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("productCodeSnapshot asc")
    private List<PhysicalInventoryItem> items = new ArrayList<>();

    protected PhysicalInventorySession() {
    }

    PhysicalInventorySession(String code, String reason, String actor, String role, LocalDateTime createdAt) {
        this.code = code;
        this.reason = reason.trim();
        this.createdBy = actor;
        this.createdByRole = role;
        this.createdAt = createdAt;
        this.status = PhysicalInventoryStatus.OPEN;
    }

    void addItem(PhysicalInventoryItem item) {
        items.add(item);
    }

    void submit(String actor, String role, LocalDateTime timestamp) {
        requireStatus(PhysicalInventoryStatus.OPEN, "La sessione non è più modificabile.");
        if (items.stream().anyMatch(item -> !item.isCounted())) {
            throw new IllegalStateException("Registra il conteggio di tutti i prodotti prima dell'invio.");
        }
        status = PhysicalInventoryStatus.SUBMITTED;
        submittedAt = timestamp;
        submittedBy = actor;
        submittedByRole = role;
    }

    void approve(String actor, String role, String approvalReason, LocalDateTime timestamp) {
        requireStatus(PhysicalInventoryStatus.SUBMITTED, "La sessione non è disponibile per l'approvazione.");
        if (submittedBy != null && submittedBy.equalsIgnoreCase(actor)) {
            throw new IllegalStateException("Chi invia il conteggio non può approvare la stessa sessione.");
        }
        this.status = PhysicalInventoryStatus.APPROVED;
        this.approvedAt = timestamp;
        this.approvedBy = actor;
        this.approvedByRole = role;
        this.approvalReason = approvalReason.trim();
        items.forEach(PhysicalInventoryItem::close);
    }

    void cancel(String actor, String role, String cancellationReason, LocalDateTime timestamp) {
        if (status != PhysicalInventoryStatus.OPEN && status != PhysicalInventoryStatus.SUBMITTED) {
            throw new IllegalStateException("La sessione conclusa non può essere annullata.");
        }
        this.status = PhysicalInventoryStatus.CANCELED;
        this.canceledAt = timestamp;
        this.canceledBy = actor;
        this.canceledByRole = role;
        this.cancellationReason = cancellationReason.trim();
        items.forEach(PhysicalInventoryItem::close);
    }

    void requireOpen() {
        requireStatus(PhysicalInventoryStatus.OPEN, "Il conteggio può essere aggiornato soltanto in una sessione aperta.");
    }

    PhysicalInventoryItem requireItem(Long itemId) {
        return items.stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Riga di inventario non trovata."));
    }

    private void requireStatus(PhysicalInventoryStatus required, String message) {
        if (status != required) {
            throw new IllegalStateException(message);
        }
    }

    public Long getId() { return id; }
    public long getVersion() { return version; }
    public String getCode() { return code; }
    public PhysicalInventoryStatus getStatus() { return status; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
    public String getCreatedByRole() { return createdByRole; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public String getSubmittedBy() { return submittedBy; }
    public String getSubmittedByRole() { return submittedByRole; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public String getApprovedBy() { return approvedBy; }
    public String getApprovedByRole() { return approvedByRole; }
    public String getApprovalReason() { return approvalReason; }
    public LocalDateTime getCanceledAt() { return canceledAt; }
    public String getCanceledBy() { return canceledBy; }
    public String getCanceledByRole() { return canceledByRole; }
    public String getCancellationReason() { return cancellationReason; }
    public List<PhysicalInventoryItem> getItems() { return List.copyOf(items); }
}
