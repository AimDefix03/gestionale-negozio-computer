alter table products
    add column discontinued boolean not null default false;

create index idx_products_discontinued on products (discontinued);
