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

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private CustomerOrder order;

    @Column(nullable = false)
    private String productCode;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false, length = 1200)
    private String productDescription;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    protected OrderItem() {
    }

    OrderItem(String productCode, String productName, String productDescription, int quantity, BigDecimal unitPrice) {
        this.productCode = productCode;
        this.productName = productName;
        this.productDescription = productDescription == null ? "" : productDescription.trim();
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    OrderItem(String productCode, String productName, int quantity, BigDecimal unitPrice) {
        this(productCode, productName, "", quantity, unitPrice);
    }

    void assignOrder(CustomerOrder order) {
        this.order = order;
    }

    public Long getId() { return id; }
    public String getProductCode() { return productCode; }
    public String getProductName() { return productName; }
    public String getProductDescription() { return productDescription; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getLineTotal() { return lineTotal; }
}
