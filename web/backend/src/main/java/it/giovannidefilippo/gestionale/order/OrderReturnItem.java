package it.giovannidefilippo.gestionale.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Entity
@Table(name = "order_return_items")
public class OrderReturnItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_id", nullable = false)
    private OrderReturn orderReturn;

    @Column(nullable = false, length = 120)
    private String productCode;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    protected OrderReturnItem() {
    }

    OrderReturnItem(String productCode, String productName, int quantity, BigDecimal unitPrice) {
        this.productCode = required(productCode);
        this.productName = required(productName);
        if (quantity <= 0) {
            throw new IllegalArgumentException("La quantita resa deve essere maggiore di zero.");
        }
        this.quantity = quantity;
        this.unitPrice = money(unitPrice);
        this.lineTotal = this.unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    void assignReturn(OrderReturn orderReturn) {
        if (this.orderReturn != null && this.orderReturn != orderReturn) {
            throw new IllegalStateException("La riga e gia associata a un reso.");
        }
        this.orderReturn = Objects.requireNonNull(orderReturn);
    }

    public String getProductCode() { return productCode; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getLineTotal() { return lineTotal; }

    private static String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Codice e nome prodotto sono obbligatori.");
        }
        return value.trim();
    }

    private static BigDecimal money(BigDecimal value) {
        BigDecimal amount = Objects.requireNonNull(value).setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Il prezzo della riga reso non puo essere negativo.");
        }
        return amount;
    }
}
