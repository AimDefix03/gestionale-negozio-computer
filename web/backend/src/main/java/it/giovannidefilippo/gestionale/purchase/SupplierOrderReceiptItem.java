package it.giovannidefilippo.gestionale.purchase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "supplier_order_receipt_items")
class SupplierOrderReceiptItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "receipt_id", nullable = false)
    private SupplierOrderReceipt receipt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "supplier_order_item_id", nullable = false)
    private SupplierOrderItem orderItem;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal expectedUnitCost;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal actualUnitCost;

    @Column(nullable = false, precision = 16, scale = 4)
    private BigDecimal totalCost;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal unitCostVariance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SupplierReceiptPostingStatus inventoryPostingStatus;

    private Long stockMovementId;

    protected SupplierOrderReceiptItem() {
    }

    SupplierOrderReceiptItem(SupplierOrderItem orderItem, int quantity, BigDecimal expectedUnitCost, BigDecimal actualUnitCost) {
        this.orderItem = orderItem;
        this.quantity = quantity;
        this.expectedUnitCost = normalize(expectedUnitCost);
        this.actualUnitCost = normalize(actualUnitCost);
        this.totalCost = this.actualUnitCost.multiply(BigDecimal.valueOf(quantity)).setScale(4, RoundingMode.HALF_UP);
        this.unitCostVariance = this.actualUnitCost.subtract(this.expectedUnitCost).setScale(4, RoundingMode.HALF_UP);
        this.inventoryPostingStatus = SupplierReceiptPostingStatus.PENDING;
    }

    void assignReceipt(SupplierOrderReceipt receipt) {
        this.receipt = receipt;
    }

    void markPosted(Long movementId) {
        if (movementId == null) {
            throw new IllegalArgumentException("Il movimento di magazzino collegato e obbligatorio.");
        }
        if (inventoryPostingStatus != SupplierReceiptPostingStatus.PENDING) {
            throw new IllegalStateException("La riga di ricezione e gia stata contabilizzata nel ledger di magazzino.");
        }
        this.stockMovementId = movementId;
        this.inventoryPostingStatus = SupplierReceiptPostingStatus.POSTED;
    }

    Long getId() { return id; }
    SupplierOrderItem getOrderItem() { return orderItem; }
    int getQuantity() { return quantity; }
    BigDecimal getExpectedUnitCost() { return expectedUnitCost; }
    BigDecimal getActualUnitCost() { return actualUnitCost; }
    BigDecimal getTotalCost() { return totalCost; }
    BigDecimal getUnitCostVariance() { return unitCostVariance; }
    SupplierReceiptPostingStatus getInventoryPostingStatus() { return inventoryPostingStatus; }
    Long getStockMovementId() { return stockMovementId; }

    private static BigDecimal normalize(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException("Il costo unitario ricevuto non puo essere negativo.");
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }
}
