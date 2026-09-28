package it.giovannidefilippo.gestionale.purchase;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "supplier_order_receipts")
class SupplierOrderReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @ManyToOne(optional = false)
    @JoinColumn(name = "supplier_order_id", nullable = false)
    private SupplierOrder order;

    @Column(nullable = false, length = 900)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    @Column(nullable = false)
    private String receivedBy;

    @Column(nullable = false)
    private String receivedByRole;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<SupplierOrderReceiptItem> items = new ArrayList<>();

    protected SupplierOrderReceipt() {
    }

    SupplierOrderReceipt(String code, String reason, LocalDateTime receivedAt, String actor, String actorRole, List<SupplierOrderReceiptItem> items) {
        this.code = code;
        this.reason = required(reason, "La causale di ricezione e obbligatoria.");
        this.receivedAt = receivedAt;
        this.receivedBy = required(actor, "L'operatore di ricezione e obbligatorio.");
        this.receivedByRole = required(actorRole, "Il ruolo dell'operatore e obbligatorio.");
        items.forEach(this::addItem);
    }

    void assignOrder(SupplierOrder order) {
        this.order = order;
    }

    private void addItem(SupplierOrderReceiptItem item) {
        item.assignReceipt(this);
        items.add(item);
    }

    Long getId() { return id; }
    String getCode() { return code; }
    String getReason() { return reason; }
    LocalDateTime getReceivedAt() { return receivedAt; }
    String getReceivedBy() { return receivedBy; }
    String getReceivedByRole() { return receivedByRole; }
    List<SupplierOrderReceiptItem> getItems() { return List.copyOf(items); }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }
}
