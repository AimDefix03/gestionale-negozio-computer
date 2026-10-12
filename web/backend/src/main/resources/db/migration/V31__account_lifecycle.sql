alter table user_accounts add column enabled boolean not null default true;
alter table user_accounts add column disabled_at timestamp(6);
alter table user_accounts add column disabled_by varchar(255);
alter table user_accounts add column disabled_reason varchar(900);

alter table user_accounts
    add constraint chk_user_accounts_lifecycle
        check (
            (enabled = true and disabled_at is null and disabled_by is null and disabled_reason is null)
            or
            (enabled = false and disabled_at is not null and char_length(trim(disabled_by)) > 0 and char_length(trim(disabled_reason)) > 0)
        );

create index idx_user_accounts_enabled_role
    on user_accounts (enabled, role);
