package it.giovannidefilippo.gestionale.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 1200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory category;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String productType;

    private String usageContext;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int reservedQuantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discount;

    @Column(nullable = false)
    private boolean discontinued;

    protected Product() {
    }

    Product(ProductRequest request) {
        update(request);
    }

    void update(ProductRequest request) {
        if (request.quantity() < reservedQuantity) {
            throw new IllegalArgumentException("La giacenza fisica non puo essere inferiore allo stock riservato.");
        }
        this.code = clean(request.code());
        this.name = clean(request.name());
        this.description = clean(request.description());
        this.category = request.category();
        this.brand = clean(request.brand());
        this.productType = clean(request.productType());
        this.usageContext = optional(request.usageContext());
        this.quantity = request.quantity();
        this.price = request.price().setScale(2, RoundingMode.HALF_UP);
        this.discount = request.discount().setScale(2, RoundingMode.HALF_UP);
    }

    void updateQuantity(int quantity) {
        this.quantity = quantity;
    }

    void discontinue() {
        this.discontinued = true;
    }

    void reserveQuantity(int quantity) {
        if (getAvailableQuantity() < quantity) {
            throw new IllegalArgumentException("Scorte vendibili insufficienti per il prodotto " + code + ".");
        }
        this.reservedQuantity += quantity;
    }

    void releaseReservedQuantity(int quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalArgumentException("La quantità da liberare supera lo stock riservato.");
        }
        this.reservedQuantity -= quantity;
    }

    void fulfillReservedQuantity(int quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalArgumentException("La quantità da evadere supera lo stock riservato.");
        }
        if (this.quantity < quantity) {
            throw new IllegalArgumentException("La giacenza fisica non e sufficiente per evadere il prodotto " + code + ".");
        }
        this.quantity -= quantity;
        this.reservedQuantity -= quantity;
    }

    public Long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public String getBrand() {
        return brand;
    }

    public String getProductType() {
        return productType;
    }

    public String getUsageContext() {
        return usageContext;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public int getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public boolean isDiscontinued() {
        return discontinued;
    }

    public BigDecimal getDiscountedPrice() {
        BigDecimal multiplier = BigDecimal.ONE.subtract(discount.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        return price.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private static String clean(String value) {
        return value.trim();
    }

    private static String optional(String value) {
        return value == null ? "" : value.trim();
    }
}
