do $$
begin
    if exists (
        select 1
        from auth_sessions
        where account_id is null
           or credential_version is null
           or revoked_at is null
    ) then
        raise exception 'V20 must bind and revoke every migrated session';
    end if;

    if not exists (
        select 1
        from auth_sessions
        where id = 1211
          and account_id = 1203
          and username_snapshot = 'cliente.riusato'
          and credential_version = 1
    ) then
        raise exception 'V20 did not bind the reused customer session to its stable account';
    end if;

    if not exists (
        select 1
        from auth_sessions
        where id = 2401
          and account_id = 1204
          and username_snapshot = 'operatore.fixture'
          and credential_version = 1
    ) then
        raise exception 'V20 did not bind the operational session to its stable account';
    end if;

    if exists (
        select 1
        from auth_sessions
        where username_snapshot = 'utente.rimosso'
    ) then
        raise exception 'V20 retained an orphan session subject';
    end if;

    insert into user_accounts (
        id,
        username,
        password_salt,
        password_hash,
        role,
        credential_version
    ) values (
        2998,
        'cascade.fixture',
        'fixture-salt-2998',
        'fixture-hash-2998-not-a-credential',
        'CUSTOMER',
        1
    );

    insert into auth_sessions (
        id,
        token_hash,
        username_snapshot,
        account_id,
        role,
        credential_version,
        created_at,
        expires_at,
        last_used_at,
        revoked_at
    ) values (
        2999,
        repeat('9', 64),
        'cascade.fixture',
        2998,
        'CUSTOMER',
        1,
        timestamp '2026-01-20 10:00:00',
        timestamp '2026-01-20 11:00:00',
        timestamp '2026-01-20 10:00:00',
        timestamp '2026-01-20 10:30:00'
    );

    delete from user_accounts where id = 2998;

    if exists (select 1 from auth_sessions where id = 2999) then
        raise exception 'V20 account deletion did not cascade to sessions';
    end if;
end $$;
