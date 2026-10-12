do $fixture$
begin
    if exists (select 1 from auth_sessions where last_used_at is null) then
        raise exception 'V18 fixture requires session activity timestamps';
    end if;
    if not exists (
        select 1 from company_settings
        where id = 1 and legal_name = 'Impresa Fixture Srl' and email like '%@example.invalid'
    ) then
        raise exception 'V18 fixture requires anonymized company settings';
    end if;
    if not exists (
        select 1 from fiscal_documents
        where type = 'SIMULATED_INVOICE' and fiscal_year = 2026 and sequence_number = 1
    ) then
        raise exception 'V18 fixture must preserve the legacy numbering collision candidate';
    end if;
    if exists (
        select 1 from document_number_counters
        where document_type = 'SIMULATED_INVOICE' and fiscal_year = 2026
    ) then
        raise exception 'V18 fixture must keep the legacy counter gap reproducible';
    end if;
    if not exists (
        select 1 from fiscal_documents
        where code = 'FIX-FS-2025-0001' and fiscal_year = 2025
    ) then
        raise exception 'V18 fixture requires a year-boundary document';
    end if;
end
$fixture$;
