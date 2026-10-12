alter table customer_orders
    add column customer_type varchar(32) not null default 'LEGACY_UNRESOLVED';

alter table customer_orders
    add constraint chk_customer_orders_customer_type
        check (customer_type in ('SELF_SERVICE', 'REGISTERED', 'WALK_IN', 'LEGACY_UNRESOLVED'));

create index idx_customer_orders_customer_type
    on customer_orders (customer_type);

alter table order_items
    add column product_description varchar(1200) not null default '';
