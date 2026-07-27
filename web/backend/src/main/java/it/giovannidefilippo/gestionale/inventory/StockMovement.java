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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockMovementType type;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int previousQuantity;

    @Column(nullable = false)
    private int newQuantity;

    @Column(nullable = false, length = 900)
    private String reason;

    protected StockMovement() {
    }

    StockMovement(String actor, String role, String productCode, String productName, StockMovementType type, int quantity, int previousQuantity, int newQuantity, String reason, LocalDateTime timestamp) {
        this.timestamp = timestamp;
        this.actor = clean(actor);
        this.role = clean(role);
        this.productCode = productCode;
        this.productName = productName;
        this.type = type;
        this.quantity = quantity;
        this.previousQuantity = previousQuantity;
        this.newQuantity = newQuantity;
        this.reason = reason.trim();
    }

    public Long getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getActor() { return actor; }
    public String getRole() { return role; }
    public String getProductCode() { return productCode; }
    public String getProductName() { return productName; }
    public StockMovementType getType() { return type; }
    public int getQuantity() { return quantity; }
    public int getPreviousQuantity() { return previousQuantity; }
    public int getNewQuantity() { return newQuantity; }
    public String getReason() { return reason; }

    private static String clean(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}
