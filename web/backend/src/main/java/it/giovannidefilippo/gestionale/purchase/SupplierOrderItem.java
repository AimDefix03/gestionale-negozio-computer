package it.giovannidefilippo.gestionale.purchase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "supplier_order_items")
class SupplierOrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "supplier_order_id", nullable = false)
    private SupplierOrder order;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String productCodeSnapshot;

    @Column(nullable = false)
    private String productNameSnapshot;

    @Column(nullable = false)
    private int orderedQuantity;

    @Column(nullable = false)
    private int receivedQuantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;

    @Column(nullable = false)
    private LocalDate expectedDeliveryDate;

    protected SupplierOrderItem() {
    }

    SupplierOrderItem(Long productId, String productCode, String productName, int quantity, BigDecimal unitPrice, LocalDate expectedDeliveryDate) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La quantita ordinata deve essere positiva.");
        }
        this.productId = productId;
        this.productCodeSnapshot = required(productCode, "Il codice prodotto e obbligatorio.");
        this.productNameSnapshot = required(productName, "Il nome prodotto e obbligatorio.");
        this.orderedQuantity = quantity;
        this.receivedQuantity = 0;
        this.unitPrice = money(unitPrice);
        this.lineTotal = this.unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    void assignOrder(SupplierOrder order) {
        this.order = order;
    }

    void receive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La quantita ricevuta deve essere positiva.");
        }
        if (quantity > remainingQuantity()) {
            throw new IllegalStateException("La quantita ricevuta supera il residuo della riga " + productCodeSnapshot + ".");
        }
        receivedQuantity += quantity;
    }

    int remainingQuantity() {
        return orderedQuantity - receivedQuantity;
    }

    boolean isComplete() {
        return receivedQuantity == orderedQuantity;
    }

    Long getId() { return id; }
    Long getProductId() { return productId; }
    String getProductCodeSnapshot() { return productCodeSnapshot; }
    String getProductNameSnapshot() { return productNameSnapshot; }
    int getOrderedQuantity() { return orderedQuantity; }
    int getReceivedQuantity() { return receivedQuantity; }
    BigDecimal getUnitPrice() { return unitPrice; }
    BigDecimal getLineTotal() { return lineTotal; }
    LocalDate getExpectedDeliveryDate() { return expectedDeliveryDate; }

    private static BigDecimal money(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException("Il prezzo concordato non puo essere negativo.");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }
}
