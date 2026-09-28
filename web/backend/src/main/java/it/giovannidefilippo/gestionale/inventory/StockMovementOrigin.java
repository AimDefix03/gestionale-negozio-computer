package it.giovannidefilippo.gestionale.inventory;

public enum StockMovementOrigin {
    LEGACY,
    MIGRATION_BASELINE,
    MANUAL_INITIAL_BALANCE,
    MANUAL_ADJUSTMENT,
    MANUAL_MOVEMENT,
    ORDER_FULFILLMENT,
    CUSTOMER_RETURN,
    SUPPLIER_ORDER_RECEIPT,
    PHYSICAL_INVENTORY
}
