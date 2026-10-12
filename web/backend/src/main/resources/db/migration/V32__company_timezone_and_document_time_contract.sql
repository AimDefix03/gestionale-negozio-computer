alter table company_settings
    add column time_zone varchar(64) not null default 'Europe/Rome';

alter table company_settings
    add constraint chk_company_settings_time_zone_not_blank
        check (char_length(trim(time_zone)) > 0);

alter table fiscal_documents
    add column company_snapshot_time_zone varchar(64) not null default 'UTC';

alter table fiscal_documents
    add constraint chk_fiscal_documents_snapshot_time_zone_not_blank
        check (char_length(trim(company_snapshot_time_zone)) > 0);
