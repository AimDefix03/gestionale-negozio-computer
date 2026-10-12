do $$
begin
    if not exists (
        select 1
        from user_accounts
        where username = 'operatore.fixture'
          and role = 'EMPLOYEE'
          and provisioning_source = 'SELF_SERVICE'
          and operational_access_verified = false
    ) then
        raise exception 'Account operativo self-service non classificato per la revisione';
    end if;

    if exists (
        select 1
        from auth_sessions session
        join user_accounts account on lower(account.username) = lower(session.username)
        where account.role <> 'CUSTOMER'
          and account.operational_access_verified = false
          and session.revoked_at is null
    ) then
        raise exception 'Esiste ancora una sessione attiva per un account operativo non verificato';
    end if;

    if not exists (
        select 1
        from audit_events
        where action = 'QUARANTINE_OPERATIONAL_ACCOUNT'
          and target = 'operatore.fixture'
          and severity = 'CRITICAL'
    ) then
        raise exception 'Evidenza audit della quarantena non conservata';
    end if;

    if not exists (
        select 1
        from stock_movements
        where actor = 'operatore.fixture'
    ) or not exists (
        select 1
        from fiscal_documents
        where created_by = 'operatore.fixture'
    ) then
        raise exception 'Evidenze operative storiche non preservate';
    end if;

    if exists (
        select 1
        from user_accounts
        where role in ('ADMIN', 'SUPER_ADMIN')
          and operational_access_verified = false
    ) then
        raise exception 'Account amministrativo legittimo bloccato dalla migrazione';
    end if;
end $$;
