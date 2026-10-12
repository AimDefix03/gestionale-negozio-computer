create table company_settings (
    id integer primary key,
    legal_name varchar(160) not null,
    tax_code varchar(32) not null,
    vat_number varchar(32) not null,
    email varchar(160) not null,
    phone varchar(40) not null,
    address varchar(300) not null,
    postal_code varchar(16) not null,
    city varchar(120) not null,
    province varchar(8) not null,
    country_code varchar(2) not null,
    default_vat_rate numeric(5, 4) not null,
    invoice_prefix varchar(8) not null,
    credit_note_prefix varchar(8) not null,
    number_padding integer not null,
    updated_at timestamp(6) not null,
    updated_by varchar(255) not null,
    version bigint not null,
    constraint chk_company_settings_singleton check (id = 1),
    constraint chk_company_settings_vat_rate check (default_vat_rate >= 0 and default_vat_rate <= 1),
    constraint chk_company_settings_invoice_prefix check (char_length(trim(invoice_prefix)) > 0),
    constraint chk_company_settings_credit_prefix check (char_length(trim(credit_note_prefix)) > 0),
    constraint chk_company_settings_distinct_prefixes check (invoice_prefix <> credit_note_prefix),
    constraint chk_company_settings_padding check (number_padding between 3 and 8),
    constraint chk_company_settings_updated_by check (char_length(trim(updated_by)) > 0)
);

insert into company_settings (
    id, legal_name, tax_code, vat_number, email, phone, address, postal_code, city, province,
    country_code, default_vat_rate, invoice_prefix, credit_note_prefix, number_padding,
    updated_at, updated_by, version
) values (
    1, '', '', '', '', '', '', '', '', '', '', 0.2200, 'FS', 'NC', 4,
    current_timestamp, 'SYSTEM', 0
);

create table document_number_counters (
    document_type varchar(255) not null,
    fiscal_year integer not null,
    next_value bigint not null,
    version bigint not null,
    primary key (document_type, fiscal_year),
    constraint chk_document_counters_year check (fiscal_year >= 2000),
    constraint chk_document_counters_next_value check (next_value > 0)
);

alter table fiscal_documents add column fiscal_year integer;
alter table fiscal_documents add column sequence_number bigint;
alter table fiscal_documents add column document_prefix varchar(8) not null default 'LEGACY';
alter table fiscal_documents add column company_snapshot_legal_name varchar(160) not null default '';
alter table fiscal_documents add column company_snapshot_tax_code varchar(32) not null default '';
alter table fiscal_documents add column company_snapshot_vat_number varchar(32) not null default '';
alter table fiscal_documents add column company_snapshot_email varchar(160) not null default '';
alter table fiscal_documents add column company_snapshot_phone varchar(40) not null default '';
alter table fiscal_documents add column company_snapshot_address varchar(300) not null default '';
alter table fiscal_documents add column company_snapshot_postal_code varchar(16) not null default '';
alter table fiscal_documents add column company_snapshot_city varchar(120) not null default '';
alter table fiscal_documents add column company_snapshot_province varchar(8) not null default '';
alter table fiscal_documents add column company_snapshot_country_code varchar(2) not null default '';
alter table fiscal_documents alter column vat_rate type numeric(5, 4);

update fiscal_documents
set fiscal_year = extract(year from created_at),
    sequence_number = id,
    document_prefix = case when type = 'SIMULATED_CREDIT_NOTE' then 'NC' else 'FS' end;

alter table fiscal_documents alter column fiscal_year set not null;
alter table fiscal_documents alter column sequence_number set not null;

alter table fiscal_documents
    add constraint uk_fiscal_documents_type_year_sequence unique (type, fiscal_year, sequence_number);

alter table fiscal_documents
    add constraint chk_fiscal_documents_fiscal_year check (fiscal_year >= 2000);

alter table fiscal_documents
    add constraint chk_fiscal_documents_sequence_positive check (sequence_number > 0);

alter table fiscal_documents
    add constraint chk_fiscal_documents_prefix_not_blank check (char_length(trim(document_prefix)) > 0);

create index idx_fiscal_documents_fiscal_year on fiscal_documents (fiscal_year);
