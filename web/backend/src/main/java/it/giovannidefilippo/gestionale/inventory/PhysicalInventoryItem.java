package it.giovannidefilippo.gestionale.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "physical_inventory_items")
class PhysicalInventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private PhysicalInventorySession session;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String productCodeSnapshot;

    @Column(nullable = false)
    private String productNameSnapshot;

    @Column(nullable = false)
    private int theoreticalQuantitySnapshot;

    @Column(nullable = false)
    private int reservedQuantitySnapshot;

    @Column(nullable = false)
    private long productVersionSnapshot;

    private Integer countedQuantity;
    private Integer theoreticalQuantityAtCount;
    private Integer reservedQuantityAtCount;
    private Long productVersionAtCount;
    private Integer differenceQuantity;
    private LocalDateTime countedAt;
    private String countedBy;
    private String countedByRole;

    @Column(length = 900)
    private String countNote;

    private Integer quantityBeforeApproval;
    private Integer quantityAfterApproval;
    private Integer reservedQuantityAtApproval;
    private Integer compensatedMovementDelta;
    private Long stockMovementId;
    private Boolean activeMarker;

    protected PhysicalInventoryItem() {
    }

    PhysicalInventoryItem(PhysicalInventorySession session, Long productId, String productCode, String productName, int quantity, int reservedQuantity, long productVersion) {
        this.session = session;
        this.productId = productId;
        this.productCodeSnapshot = productCode;
        this.productNameSnapshot = productName;
        this.theoreticalQuantitySnapshot = quantity;
        this.reservedQuantitySnapshot = reservedQuantity;
        this.productVersionSnapshot = productVersion;
        this.activeMarker = Boolean.TRUE;
    }

    void recordCount(int countedQuantity, int theoreticalQuantity, int reservedQuantity, long productVersion, String actor, String role, String note, LocalDateTime timestamp) {
        if (countedQuantity < 0) {
            throw new IllegalArgumentException("La quantità conteggiata non può essere negativa.");
        }
        this.countedQuantity = countedQuantity;
        this.theoreticalQuantityAtCount = theoreticalQuantity;
        this.reservedQuantityAtCount = reservedQuantity;
        this.productVersionAtCount = productVersion;
        this.differenceQuantity = countedQuantity - theoreticalQuantity;
        this.countedAt = timestamp;
        this.countedBy = actor;
        this.countedByRole = role;
        this.countNote = note == null || note.isBlank() ? null : note.trim();
    }

    void recordApproval(int previousQuantity, int newQuantity, int reservedQuantity, Long stockMovementId) {
        this.quantityBeforeApproval = previousQuantity;
        this.quantityAfterApproval = newQuantity;
        this.reservedQuantityAtApproval = reservedQuantity;
        this.compensatedMovementDelta = previousQuantity - theoreticalQuantityAtCount;
        this.stockMovementId = stockMovementId;
    }

    void close() {
        this.activeMarker = null;
    }

    boolean isCounted() { return countedQuantity != null; }
    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getProductCodeSnapshot() { return productCodeSnapshot; }
    public String getProductNameSnapshot() { return productNameSnapshot; }
    public int getTheoreticalQuantitySnapshot() { return theoreticalQuantitySnapshot; }
    public int getReservedQuantitySnapshot() { return reservedQuantitySnapshot; }
    public long getProductVersionSnapshot() { return productVersionSnapshot; }
    public Integer getCountedQuantity() { return countedQuantity; }
    public Integer getTheoreticalQuantityAtCount() { return theoreticalQuantityAtCount; }
    public Integer getReservedQuantityAtCount() { return reservedQuantityAtCount; }
    public Long getProductVersionAtCount() { return productVersionAtCount; }
    public Integer getDifferenceQuantity() { return differenceQuantity; }
    public LocalDateTime getCountedAt() { return countedAt; }
    public String getCountedBy() { return countedBy; }
    public String getCountedByRole() { return countedByRole; }
    public String getCountNote() { return countNote; }
    public Integer getQuantityBeforeApproval() { return quantityBeforeApproval; }
    public Integer getQuantityAfterApproval() { return quantityAfterApproval; }
    public Integer getReservedQuantityAtApproval() { return reservedQuantityAtApproval; }
    public Integer getCompensatedMovementDelta() { return compensatedMovementDelta; }
    public Long getStockMovementId() { return stockMovementId; }
}
