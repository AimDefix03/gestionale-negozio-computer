alter table products
    add column reserved_quantity integer not null default 0;

alter table products
    add constraint chk_products_reserved_quantity_not_negative check (reserved_quantity >= 0);

alter table products
    add constraint chk_products_reserved_not_above_quantity check (reserved_quantity <= quantity);
