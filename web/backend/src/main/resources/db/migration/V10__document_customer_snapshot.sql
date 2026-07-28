alter table fiscal_documents
    add column customer_snapshot_code varchar(255) not null default '';

alter table fiscal_documents
    add column customer_snapshot_name varchar(255) not null default '';

alter table fiscal_documents
    add column customer_snapshot_tax_code varchar(255) not null default '';

alter table fiscal_documents
    add column customer_snapshot_vat_number varchar(255) not null default '';

alter table fiscal_documents
    add column customer_snapshot_email varchar(255) not null default '';

alter table fiscal_documents
    add column customer_snapshot_phone varchar(255) not null default '';

alter table fiscal_documents
    add column customer_snapshot_address varchar(600) not null default '';

alter table fiscal_documents
    add column customer_snapshot_city varchar(255) not null default '';

update fiscal_documents
set customer_snapshot_name = customer
where customer_snapshot_name = '';
