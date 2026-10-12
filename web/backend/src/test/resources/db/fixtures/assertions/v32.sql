do $$
declare
    invalid_company_time_zones bigint;
    invalid_document_time_zones bigint;
    non_utc_legacy_documents bigint;
    company_constraint_count bigint;
    document_constraint_count bigint;
begin
    select count(*) into invalid_company_time_zones
    from company_settings
    where nullif(trim(time_zone), '') is null;

    if invalid_company_time_zones <> 0 then
        raise exception 'V32 leaves % company settings without a time zone', invalid_company_time_zones;
    end if;

    select count(*) into invalid_document_time_zones
    from fiscal_documents
    where nullif(trim(company_snapshot_time_zone), '') is null;

    if invalid_document_time_zones <> 0 then
        raise exception 'V32 leaves % fiscal documents without a snapshot time zone', invalid_document_time_zones;
    end if;

    select count(*) into non_utc_legacy_documents
    from fiscal_documents
    where company_snapshot_time_zone <> 'UTC';

    if non_utc_legacy_documents <> 0 then
        raise exception 'V32 must preserve unknown legacy document time zones as UTC, but % differ', non_utc_legacy_documents;
    end if;

    select count(*) into company_constraint_count
    from pg_constraint
    where conrelid = 'company_settings'::regclass
      and conname = 'chk_company_settings_time_zone_not_blank';

    if company_constraint_count <> 1 then
        raise exception 'V32 must install chk_company_settings_time_zone_not_blank exactly once';
    end if;

    select count(*) into document_constraint_count
    from pg_constraint
    where conrelid = 'fiscal_documents'::regclass
      and conname = 'chk_fiscal_documents_snapshot_time_zone_not_blank';

    if document_constraint_count <> 1 then
        raise exception 'V32 must install chk_fiscal_documents_snapshot_time_zone_not_blank exactly once';
    end if;
end $$;
