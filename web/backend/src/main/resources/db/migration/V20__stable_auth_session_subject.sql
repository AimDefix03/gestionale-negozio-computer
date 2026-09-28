alter table user_accounts
    add column credential_version bigint not null default 1;

alter table auth_sessions
    rename column username to username_snapshot;

alter table auth_sessions
    add column account_id bigint;

alter table auth_sessions
    add column credential_version bigint;

update auth_sessions session
set account_id = (
        select account.id
        from user_accounts account
        where account.username = session.username_snapshot
    ),
    credential_version = (
        select account.credential_version
        from user_accounts account
        where account.username = session.username_snapshot
    );

update auth_sessions
set revoked_at = coalesce(revoked_at, current_timestamp);

delete from auth_sessions
where account_id is null;

alter table auth_sessions
    alter column account_id set not null;

alter table auth_sessions
    alter column credential_version set not null;

alter table auth_sessions
    add constraint fk_auth_sessions_account
        foreign key (account_id)
        references user_accounts (id)
        on delete cascade;

drop index if exists idx_auth_sessions_username;

create index idx_auth_sessions_account_id
    on auth_sessions (account_id);
