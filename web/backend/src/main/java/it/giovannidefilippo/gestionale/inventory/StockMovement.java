package it.giovannidefilippo.gestionale.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "stock_movements")
public class StockMovement {
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
    private String productCode;

    @Column(nullable = false)
    private String productName;

    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockMovementType type;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int previousQuantity;

    @Column(nullable = false)
    private int newQuantity;

    @Column(nullable = false)
    private int deltaQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockMovementOrigin origin;

    @Column(nullable = false)
    private boolean authoritative;

    private Boolean baselineMarker;

    @Column(nullable = false, length = 900)
    private String reason;

    private Long supplierOrderId;

    private Long supplierOrderReceiptId;

    private Long supplierOrderReceiptItemId;

    @Column(precision = 14, scale = 4)
    private BigDecimal unitCost;

    @Column(precision = 16, scale = 4)
    private BigDecimal totalCost;

    @Column(precision = 14, scale = 4)
    private BigDecimal averageCostBefore;

    @Column(precision = 14, scale = 4)
    private BigDecimal averageCostAfter;

    private Integer costedQuantityBefore;

    private Integer costedQuantityAfter;

    private Long physicalInventorySessionId;

    private Long physicalInventoryItemId;

    protected StockMovement() {
    }

    StockMovement(Long productId, String actor, String role, String productCode, String productName, StockMovementType type, int deltaQuantity, int previousQuantity, int newQuantity, String reason, LocalDateTime timestamp, StockMovementOrigin origin, boolean baseline) {
        this.timestamp = timestamp;
        this.actor = clean(actor);
        this.role = clean(role);
        this.productCode = productCode;
        this.productName = productName;
        this.productId = productId;
        this.type = type;
        this.quantity = Math.abs(deltaQuantity);
        this.previousQuantity = previousQuantity;
        this.newQuantity = newQuantity;
        this.deltaQuantity = deltaQuantity;
        this.origin = origin;
        this.authoritative = true;
        this.baselineMarker = baseline ? Boolean.TRUE : null;
        this.reason = reason.trim();
    }

    StockMovement(
            Long productId,
            String actor,
            String role,
            String productCode,
            String productName,
            int receivedQuantity,
            int previousQuantity,
            int newQuantity,
            String reason,
            LocalDateTime timestamp,
            boolean baseline,
            Long supplierOrderId,
            Long supplierOrderReceiptId,
            Long supplierOrderReceiptItemId,
            BigDecimal unitCost,
            BigDecimal totalCost,
            BigDecimal averageCostBefore,
            BigDecimal averageCostAfter,
            int costedQuantityBefore,
            int costedQuantityAfter
    ) {
        this(productId, actor, role, productCode, productName, StockMovementType.PURCHASE_RECEIPT,
                receivedQuantity, previousQuantity, newQuantity, reason, timestamp,
                StockMovementOrigin.SUPPLIER_ORDER_RECEIPT, baseline);
        this.supplierOrderId = supplierOrderId;
        this.supplierOrderReceiptId = supplierOrderReceiptId;
        this.supplierOrderReceiptItemId = supplierOrderReceiptItemId;
        this.unitCost = unitCost;
        this.totalCost = totalCost;
        this.averageCostBefore = averageCostBefore;
        this.averageCostAfter = averageCostAfter;
        this.costedQuantityBefore = costedQuantityBefore;
        this.costedQuantityAfter = costedQuantityAfter;
    }

    StockMovement(
            Long productId,
            String actor,
            String role,
            String productCode,
            String productName,
            StockMovementType type,
            int deltaQuantity,
            int previousQuantity,
            int newQuantity,
            String reason,
            LocalDateTime timestamp,
            Long physicalInventorySessionId,
            Long physicalInventoryItemId
    ) {
        this(productId, actor, role, productCode, productName, type, deltaQuantity, previousQuantity, newQuantity,
                reason, timestamp, StockMovementOrigin.PHYSICAL_INVENTORY, false);
        this.physicalInventorySessionId = physicalInventorySessionId;
        this.physicalInventoryItemId = physicalInventoryItemId;
    }

    public Long getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getActor() { return actor; }
    public String getRole() { return role; }
    public String getProductCode() { return productCode; }
    public String getProductName() { return productName; }
    public Long getProductId() { return productId; }
    public StockMovementType getType() { return type; }
    public int getQuantity() { return quantity; }
    public int getPreviousQuantity() { return previousQuantity; }
    public int getNewQuantity() { return newQuantity; }
    public int getDeltaQuantity() { return deltaQuantity; }
    public StockMovementOrigin getOrigin() { return origin; }
    public boolean isAuthoritative() { return authoritative; }
    public boolean isBaseline() { return Boolean.TRUE.equals(baselineMarker); }
    public String getReason() { return reason; }
    public Long getSupplierOrderId() { return supplierOrderId; }
    public Long getSupplierOrderReceiptId() { return supplierOrderReceiptId; }
    public Long getSupplierOrderReceiptItemId() { return supplierOrderReceiptItemId; }
    public BigDecimal getUnitCost() { return unitCost; }
    public BigDecimal getTotalCost() { return totalCost; }
    public BigDecimal getAverageCostBefore() { return averageCostBefore; }
    public BigDecimal getAverageCostAfter() { return averageCostAfter; }
    public Integer getCostedQuantityBefore() { return costedQuantityBefore; }
    public Integer getCostedQuantityAfter() { return costedQuantityAfter; }
    public Long getPhysicalInventorySessionId() { return physicalInventorySessionId; }
    public Long getPhysicalInventoryItemId() { return physicalInventoryItemId; }

    private static String clean(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}
