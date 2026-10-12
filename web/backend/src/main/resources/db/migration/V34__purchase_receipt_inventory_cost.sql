alter table products add column last_purchase_cost numeric(14, 4);
alter table products add column average_purchase_cost numeric(14, 4);
alter table products add column costed_quantity integer not null default 0;

alter table products
    add constraint chk_products_purchase_costs
        check (
            costed_quantity >= 0
            and costed_quantity <= quantity
            and (last_purchase_cost is null or last_purchase_cost >= 0)
            and (average_purchase_cost is null or average_purchase_cost >= 0)
            and ((costed_quantity = 0 and average_purchase_cost is null) or (costed_quantity > 0 and average_purchase_cost is not null))
        );

alter table supplier_order_receipt_items add column expected_unit_cost numeric(14, 4);
alter table supplier_order_receipt_items add column actual_unit_cost numeric(14, 4);
alter table supplier_order_receipt_items add column total_cost numeric(16, 4);
alter table supplier_order_receipt_items add column unit_cost_variance numeric(14, 4);
alter table supplier_order_receipt_items add column inventory_posting_status varchar(32) not null default 'PENDING';
alter table supplier_order_receipt_items add column stock_movement_id bigint;

update supplier_order_receipt_items receipt_item
set expected_unit_cost = (
        select order_item.unit_price
        from supplier_order_items order_item
        where order_item.id = receipt_item.supplier_order_item_id
    ),
    actual_unit_cost = (
        select order_item.unit_price
        from supplier_order_items order_item
        where order_item.id = receipt_item.supplier_order_item_id
    ),
    total_cost = receipt_item.quantity * (
        select order_item.unit_price
        from supplier_order_items order_item
        where order_item.id = receipt_item.supplier_order_item_id
    ),
    unit_cost_variance = 0,
    inventory_posting_status = 'LEGACY_UNPOSTED';

alter table supplier_order_receipt_items alter column expected_unit_cost set not null;
alter table supplier_order_receipt_items alter column actual_unit_cost set not null;
alter table supplier_order_receipt_items alter column total_cost set not null;
alter table supplier_order_receipt_items alter column unit_cost_variance set not null;

alter table stock_movements add column supplier_order_id bigint;
alter table stock_movements add column supplier_order_receipt_id bigint;
alter table stock_movements add column supplier_order_receipt_item_id bigint;
alter table stock_movements add column unit_cost numeric(14, 4);
alter table stock_movements add column total_cost numeric(16, 4);
alter table stock_movements add column average_cost_before numeric(14, 4);
alter table stock_movements add column average_cost_after numeric(14, 4);
alter table stock_movements add column costed_quantity_before integer;
alter table stock_movements add column costed_quantity_after integer;

alter table stock_movements drop constraint chk_stock_movements_origin;
alter table stock_movements drop constraint chk_stock_movements_baseline_marker;
alter table stock_movements drop constraint chk_stock_movements_authoritative_consistency;

alter table stock_movements
    add constraint chk_stock_movements_origin
        check (origin in ('LEGACY', 'MIGRATION_BASELINE', 'MANUAL_INITIAL_BALANCE', 'MANUAL_ADJUSTMENT', 'MANUAL_MOVEMENT', 'ORDER_FULFILLMENT', 'CUSTOMER_RETURN', 'SUPPLIER_ORDER_RECEIPT'));

alter table stock_movements
    add constraint chk_stock_movements_baseline_marker
        check (
            (type = 'INITIAL_BALANCE' and baseline_marker = true)
            or (type = 'PURCHASE_RECEIPT' and (baseline_marker is null or baseline_marker = true))
            or (type not in ('INITIAL_BALANCE', 'PURCHASE_RECEIPT') and baseline_marker is null)
        );

alter table stock_movements
    add constraint chk_stock_movements_authoritative_consistency
        check (
            authoritative = false
            or (
                product_id is not null
                and new_quantity = previous_quantity + delta_quantity
                and (
                    (
                        type = 'INITIAL_BALANCE'
                        and previous_quantity = 0
                        and delta_quantity >= 0
                        and quantity = delta_quantity
                    )
                    or (
                        type <> 'INITIAL_BALANCE'
                        and delta_quantity <> 0
                        and quantity = abs(delta_quantity)
                    )
                )
            )
        );

alter table stock_movements
    add constraint fk_stock_movements_supplier_order
        foreign key (supplier_order_id) references supplier_orders (id);
alter table stock_movements
    add constraint fk_stock_movements_supplier_receipt
        foreign key (supplier_order_receipt_id) references supplier_order_receipts (id);
alter table stock_movements
    add constraint fk_stock_movements_supplier_receipt_item
        foreign key (supplier_order_receipt_item_id) references supplier_order_receipt_items (id);
alter table stock_movements
    add constraint uk_stock_movements_supplier_receipt_item
        unique (supplier_order_receipt_item_id);
alter table stock_movements
    add constraint chk_stock_movements_purchase_cost
        check (
            (
                type = 'PURCHASE_RECEIPT'
                and origin = 'SUPPLIER_ORDER_RECEIPT'
                and supplier_order_id is not null
                and supplier_order_receipt_id is not null
                and supplier_order_receipt_item_id is not null
                and unit_cost is not null and unit_cost >= 0
                and total_cost is not null and total_cost = unit_cost * quantity
                and average_cost_after is not null and average_cost_after >= 0
                and costed_quantity_before is not null and costed_quantity_before >= 0
                and costed_quantity_after is not null and costed_quantity_after = costed_quantity_before + quantity
            )
            or (
                type <> 'PURCHASE_RECEIPT'
                and supplier_order_id is null
                and supplier_order_receipt_id is null
                and supplier_order_receipt_item_id is null
                and unit_cost is null
                and total_cost is null
                and average_cost_before is null
                and average_cost_after is null
                and costed_quantity_before is null
                and costed_quantity_after is null
            )
        );

alter table supplier_order_receipt_items
    add constraint fk_supplier_receipt_items_stock_movement
        foreign key (stock_movement_id) references stock_movements (id);
alter table supplier_order_receipt_items
    add constraint uk_supplier_receipt_items_stock_movement
        unique (stock_movement_id);
alter table supplier_order_receipt_items
    add constraint chk_supplier_receipt_items_cost
        check (
            expected_unit_cost >= 0
            and actual_unit_cost >= 0
            and total_cost = actual_unit_cost * quantity
            and unit_cost_variance = actual_unit_cost - expected_unit_cost
        );
alter table supplier_order_receipt_items
    add constraint chk_supplier_receipt_items_posting
        check (
            (inventory_posting_status = 'POSTED' and stock_movement_id is not null)
            or (inventory_posting_status in ('PENDING', 'LEGACY_UNPOSTED') and stock_movement_id is null)
        );

create index idx_stock_movements_supplier_receipt on stock_movements (supplier_order_receipt_id, timestamp);
create index idx_supplier_receipt_items_posting on supplier_order_receipt_items (inventory_posting_status, receipt_id);
