package it.giovannidefilippo.gestionale.product;

import it.giovannidefilippo.gestionale.common.BusinessIdentifierCanonicalizer;
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

    @Column(nullable = false, unique = true)
    private String codeCanonical;

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

    @Column(precision = 14, scale = 4)
    private BigDecimal lastPurchaseCost;

    @Column(precision = 14, scale = 4)
    private BigDecimal averagePurchaseCost;

    @Column(nullable = false)
    private int costedQuantity;

    protected Product() {
    }

    Product(ProductRequest request) {
        this.quantity = 0;
        this.reservedQuantity = 0;
        update(request);
    }

    void update(ProductRequest request) {
        this.code = BusinessIdentifierCanonicalizer.display(request.code());
        this.codeCanonical = BusinessIdentifierCanonicalizer.canonical(request.code());
        this.name = clean(request.name());
        this.description = clean(request.description());
        this.category = request.category();
        this.brand = clean(request.brand());
        this.productType = clean(request.productType());
        this.usageContext = optional(request.usageContext());
        this.price = request.price().setScale(2, RoundingMode.HALF_UP);
        this.discount = request.discount().setScale(2, RoundingMode.HALF_UP);
    }

    void updateQuantity(int quantity) {
        int removedQuantity = Math.max(0, this.quantity - quantity);
        this.quantity = quantity;
        if (removedQuantity > 0) {
            costedQuantity = Math.max(0, costedQuantity - removedQuantity);
        }
        if (costedQuantity == 0) {
            averagePurchaseCost = null;
        }
    }

    void initializeQuantity(int quantity) {
        if (this.quantity != 0 || this.reservedQuantity != 0) {
            throw new IllegalStateException("Il saldo iniziale puo essere registrato soltanto prima di altre operazioni di magazzino.");
        }
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
        updateQuantity(this.quantity - quantity);
        this.reservedQuantity -= quantity;
    }

    CostedStockReceipt receivePurchase(int receivedQuantity, BigDecimal receivedUnitCost) {
        if (receivedQuantity <= 0) {
            throw new IllegalArgumentException("La quantita ricevuta deve essere maggiore di zero.");
        }
        if (receivedUnitCost == null || receivedUnitCost.signum() < 0) {
            throw new IllegalArgumentException("Il costo unitario ricevuto non puo essere negativo.");
        }
        BigDecimal unitCost = receivedUnitCost.setScale(4, RoundingMode.HALF_UP);
        BigDecimal previousAverage = averagePurchaseCost;
        int previousPhysical = quantity;
        int previousCosted = costedQuantity;
        int newCosted = previousCosted + receivedQuantity;
        BigDecimal previousValue = previousAverage == null
                ? BigDecimal.ZERO
                : previousAverage.multiply(BigDecimal.valueOf(previousCosted));
        BigDecimal receivedValue = unitCost.multiply(BigDecimal.valueOf(receivedQuantity));
        BigDecimal newAverage = previousValue.add(receivedValue)
                .divide(BigDecimal.valueOf(newCosted), 4, RoundingMode.HALF_UP);
        quantity += receivedQuantity;
        costedQuantity = newCosted;
        lastPurchaseCost = unitCost;
        averagePurchaseCost = newAverage;
        return new CostedStockReceipt(
                id, code, name, previousPhysical, quantity, previousCosted, newCosted,
                unitCost, receivedValue.setScale(4, RoundingMode.HALF_UP), previousAverage, newAverage
        );
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

    public BigDecimal getLastPurchaseCost() {
        return lastPurchaseCost;
    }

    public BigDecimal getAveragePurchaseCost() {
        return averagePurchaseCost;
    }

    public int getCostedQuantity() {
        return costedQuantity;
    }

    public int getUncostedQuantity() {
        return quantity - costedQuantity;
    }

    public BigDecimal getCostCoveragePercentage() {
        if (quantity == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(costedQuantity)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getKnownInventoryCost() {
        if (averagePurchaseCost == null || costedQuantity == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return averagePurchaseCost.multiply(BigDecimal.valueOf(costedQuantity)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getPotentialGrossMarginOnCostedStock() {
        if (averagePurchaseCost == null || costedQuantity == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return getDiscountedPrice().subtract(averagePurchaseCost)
                .multiply(BigDecimal.valueOf(costedQuantity))
                .setScale(2, RoundingMode.HALF_UP);
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
