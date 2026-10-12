do $$
declare
    invalid_lifecycle_metadata bigint;
    disabled_historical_accounts bigint;
    lifecycle_constraint_count bigint;
    lifecycle_index_count bigint;
begin
    select count(*) into invalid_lifecycle_metadata
    from user_accounts
    where (enabled = true and (disabled_at is not null or disabled_by is not null or disabled_reason is not null))
       or (enabled = false and (disabled_at is null or nullif(trim(disabled_by), '') is null or nullif(trim(disabled_reason), '') is null));

    if invalid_lifecycle_metadata <> 0 then
        raise exception 'V31 leaves % accounts with invalid account lifecycle metadata', invalid_lifecycle_metadata;
    end if;

    select count(*) into disabled_historical_accounts
    from user_accounts
    where enabled = false;

    if disabled_historical_accounts <> 0 then
        raise exception 'V31 historical accounts must remain enabled, but % were disabled', disabled_historical_accounts;
    end if;

    select count(*) into lifecycle_constraint_count
    from pg_constraint
    where conrelid = 'user_accounts'::regclass
      and conname = 'chk_user_accounts_lifecycle';

    if lifecycle_constraint_count <> 1 then
        raise exception 'V31 must install chk_user_accounts_lifecycle exactly once';
    end if;

    select count(*) into lifecycle_index_count
    from pg_indexes
    where tablename = 'user_accounts'
      and indexname = 'idx_user_accounts_enabled_role';

    if lifecycle_index_count <> 1 then
        raise exception 'V31 must install idx_user_accounts_enabled_role exactly once';
    end if;
end $$;
