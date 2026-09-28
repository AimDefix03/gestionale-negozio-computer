alter table stock_movements add column product_id bigint;
alter table stock_movements add column delta_quantity integer;
alter table stock_movements add column origin varchar(64);
alter table stock_movements add column authoritative boolean;
alter table stock_movements add column baseline_marker boolean;

update stock_movements movement
set product_id = (
    select product.id
    from products product
    where product.code_canonical = lower(trim(movement.product_code))
);

update stock_movements
set delta_quantity = case
        when type in ('LOAD', 'RETURN') then quantity
        when type = 'UNLOAD' then -quantity
        else new_quantity - previous_quantity
    end,
    origin = 'LEGACY',
    authoritative = false,
    baseline_marker = null;

alter table stock_movements alter column delta_quantity set not null;
alter table stock_movements alter column origin set not null;
alter table stock_movements alter column authoritative set not null;

alter table stock_movements drop constraint chk_stock_movements_quantity_positive;

insert into stock_movements (
    timestamp,
    actor,
    role,
    product_code,
    product_name,
    type,
    quantity,
    previous_quantity,
    new_quantity,
    reason,
    product_id,
    delta_quantity,
    origin,
    authoritative,
    baseline_marker
)
select
    current_timestamp,
    'SYSTEM',
    'Migrazione V24',
    product.code,
    product.name,
    'INITIAL_BALANCE',
    product.quantity,
    0,
    product.quantity,
    'Saldo iniziale migrato da giacenza storica non riconciliata',
    product.id,
    product.quantity,
    'MIGRATION_BASELINE',
    true,
    true
from products product;

alter table stock_movements
    add constraint fk_stock_movements_product
        foreign key (product_id) references products (id) on delete set null;

alter table stock_movements
    add constraint uk_stock_movements_product_baseline
        unique (product_id, baseline_marker);

alter table stock_movements
    add constraint chk_stock_movements_quantity_non_negative
        check (quantity >= 0);

alter table stock_movements
    add constraint chk_stock_movements_origin
        check (origin in ('LEGACY', 'MIGRATION_BASELINE', 'MANUAL_INITIAL_BALANCE', 'MANUAL_ADJUSTMENT', 'MANUAL_MOVEMENT', 'ORDER_FULFILLMENT', 'CUSTOMER_RETURN'));

alter table stock_movements
    add constraint chk_stock_movements_baseline_marker
        check (
            (type = 'INITIAL_BALANCE' and baseline_marker = true)
            or (type <> 'INITIAL_BALANCE' and baseline_marker is null)
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

create index idx_stock_movements_product_id on stock_movements (product_id);
create index idx_stock_movements_product_authoritative on stock_movements (product_id, authoritative, timestamp);
