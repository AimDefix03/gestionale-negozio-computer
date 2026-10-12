package it.giovannidefilippo.gestionale.purchase;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "supplier_orders")
class SupplierOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private Long supplierId;

    @Column(nullable = false)
    private String supplierCodeSnapshot;

    @Column(nullable = false)
    private String supplierNameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SupplierOrderStatus status;

    @Column(nullable = false)
    private LocalDate expectedDeliveryDate;

    @Column(nullable = false, length = 1200)
    private String notes;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String createdByRole;

    private LocalDateTime sentAt;

    private LocalDateTime canceledAt;

    private String canceledBy;

    private String canceledByRole;

    @Column(length = 900)
    private String cancellationReason;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<SupplierOrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<SupplierOrderReceipt> receipts = new ArrayList<>();

    protected SupplierOrder() {
    }

    SupplierOrder(String code, Long supplierId, String supplierCode, String supplierName, LocalDate expectedDeliveryDate, String notes, List<SupplierOrderItem> items, LocalDateTime createdAt, String actor, String actorRole) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("L'ordine fornitore deve contenere almeno una riga.");
        }
        this.code = required(code, "Il codice ordine e obbligatorio.");
        this.supplierId = supplierId;
        this.supplierCodeSnapshot = required(supplierCode, "Il codice fornitore e obbligatorio.");
        this.supplierNameSnapshot = required(supplierName, "Il nome fornitore e obbligatorio.");
        this.status = SupplierOrderStatus.DRAFT;
        this.expectedDeliveryDate = expectedDeliveryDate;
        this.notes = optional(notes);
        this.total = items.stream().map(SupplierOrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.currency = "EUR";
        this.createdAt = createdAt;
        this.createdBy = required(actor, "L'operatore di creazione e obbligatorio.");
        this.createdByRole = required(actorRole, "Il ruolo dell'operatore e obbligatorio.");
        items.forEach(this::addItem);
    }

    void send(LocalDateTime timestamp) {
        requireStatus(SupplierOrderStatus.DRAFT, "Puoi inviare solo un ordine fornitore in bozza.");
        status = SupplierOrderStatus.SENT;
        sentAt = timestamp;
    }

    SupplierOrderReceipt receive(String receiptCode, String reason, Map<Long, SupplierReceiptLine> receivedLines, LocalDateTime timestamp, String actor, String actorRole) {
        if (status != SupplierOrderStatus.SENT && status != SupplierOrderStatus.PARTIALLY_RECEIVED) {
            throw new IllegalStateException("Puoi registrare ricezioni solo su ordini inviati con residuo disponibile.");
        }
        if (receivedLines == null || receivedLines.isEmpty()) {
            throw new IllegalArgumentException("Indica almeno una quantita da ricevere.");
        }
        List<SupplierOrderReceiptItem> receivedItems = new ArrayList<>();
        receivedLines.forEach((lineId, receivedLine) -> {
            SupplierOrderItem line = requireLine(lineId);
            line.receive(receivedLine.quantity());
            BigDecimal actualUnitCost = receivedLine.actualUnitCost() == null ? line.getUnitPrice() : receivedLine.actualUnitCost();
            receivedItems.add(new SupplierOrderReceiptItem(line, receivedLine.quantity(), line.getUnitPrice(), actualUnitCost));
        });
        SupplierOrderReceipt receipt = new SupplierOrderReceipt(receiptCode, reason, timestamp, actor, actorRole, receivedItems);
        receipt.assignOrder(this);
        receipts.add(receipt);
        status = items.stream().allMatch(SupplierOrderItem::isComplete)
                ? SupplierOrderStatus.RECEIVED
                : SupplierOrderStatus.PARTIALLY_RECEIVED;
        return receipt;
    }

    void cancel(String reason, LocalDateTime timestamp, String actor, String actorRole) {
        if (status != SupplierOrderStatus.DRAFT && status != SupplierOrderStatus.SENT && status != SupplierOrderStatus.PARTIALLY_RECEIVED) {
            throw new IllegalStateException("Puoi annullare solo ordini in bozza, inviati o ricevuti parzialmente.");
        }
        cancellationReason = required(reason, "La motivazione dell'annullamento e obbligatoria.");
        canceledAt = timestamp;
        canceledBy = required(actor, "L'operatore dell'annullamento e obbligatorio.");
        canceledByRole = required(actorRole, "Il ruolo dell'operatore e obbligatorio.");
        status = SupplierOrderStatus.CANCELED;
    }

    private void addItem(SupplierOrderItem item) {
        item.assignOrder(this);
        items.add(item);
    }

    private SupplierOrderItem requireLine(Long lineId) {
        return items.stream()
                .filter(item -> item.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Riga ordine fornitore non trovata."));
    }

    private void requireStatus(SupplierOrderStatus expected, String message) {
        if (status != expected) throw new IllegalStateException(message);
    }

    Long getId() { return id; }
    long getVersion() { return version; }
    String getCode() { return code; }
    Long getSupplierId() { return supplierId; }
    String getSupplierCodeSnapshot() { return supplierCodeSnapshot; }
    String getSupplierNameSnapshot() { return supplierNameSnapshot; }
    SupplierOrderStatus getStatus() { return status; }
    LocalDate getExpectedDeliveryDate() { return expectedDeliveryDate; }
    String getNotes() { return notes; }
    BigDecimal getTotal() { return total; }
    String getCurrency() { return currency; }
    LocalDateTime getCreatedAt() { return createdAt; }
    String getCreatedBy() { return createdBy; }
    String getCreatedByRole() { return createdByRole; }
    LocalDateTime getSentAt() { return sentAt; }
    LocalDateTime getCanceledAt() { return canceledAt; }
    String getCanceledBy() { return canceledBy; }
    String getCanceledByRole() { return canceledByRole; }
    String getCancellationReason() { return cancellationReason; }
    List<SupplierOrderItem> getItems() { return List.copyOf(items); }
    List<SupplierOrderReceipt> getReceipts() { return List.copyOf(receipts); }

    private static String optional(String value) {
        return value == null ? "" : value.trim();
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }
}
