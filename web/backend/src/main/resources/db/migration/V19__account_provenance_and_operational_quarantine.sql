alter table user_accounts add column provisioning_source varchar(40) not null default 'UNKNOWN';
alter table user_accounts add column operational_access_verified boolean not null default false;
alter table user_accounts add column operational_verified_at timestamp(6);
alter table user_accounts add column operational_verified_by varchar(255);

update user_accounts account
set provisioning_source = 'BOOTSTRAP'
where exists (
    select 1
    from audit_events event
    where lower(event.target) = lower(account.username)
      and event.action = 'CREATE_ACCOUNT'
      and lower(event.details) like '%bootstrap%'
);

update user_accounts account
set provisioning_source = 'ADMIN_PROVISIONED'
where exists (
    select 1
    from audit_events event
    where lower(event.target) = lower(account.username)
      and event.action = 'CREATE_ACCOUNT'
      and lower(event.details) like '%pannello admin%'
);

update user_accounts account
set provisioning_source = 'SELF_SERVICE'
where exists (
    select 1
    from audit_events event
    where lower(event.target) = lower(account.username)
      and event.action = 'CREATE_ACCOUNT'
      and (
          lower(event.actor) = 'self_service'
          or (
              lower(event.actor) = 'sistema'
              and lower(event.details) like 'registrazione pubblica%'
          )
      )
);

update user_accounts
set operational_access_verified = true,
    operational_verified_at = current_timestamp,
    operational_verified_by = case
        when role = 'CUSTOMER' then 'NOT_REQUIRED'
        when provisioning_source = 'BOOTSTRAP' then 'MIGRATION_V19_BOOTSTRAP_EVIDENCE'
        when provisioning_source = 'ADMIN_PROVISIONED' then 'MIGRATION_V19_ADMIN_EVIDENCE'
        else 'MIGRATION_V19_ROLE_GUARD'
    end
where role in ('CUSTOMER', 'ADMIN', 'SUPER_ADMIN')
   or provisioning_source in ('BOOTSTRAP', 'ADMIN_PROVISIONED');

insert into audit_events (
    timestamp,
    actor,
    role,
    action,
    target,
    details,
    category,
    severity,
    request_id,
    source,
    entity_type
)
select
    current_timestamp,
    'SYSTEM_SECURITY_MIGRATION',
    'Sistema',
    'QUARANTINE_OPERATIONAL_ACCOUNT',
    account.username,
    'Account operativo con provenienza ' || account.provisioning_source || ' posto in revisione; sessioni precedenti revocate',
    'SECURITY',
    'CRITICAL',
    '-',
    'MIGRATION V19',
    'USER_ACCOUNT'
from user_accounts account
where account.role <> 'CUSTOMER'
  and account.operational_access_verified = false;

update auth_sessions session
set revoked_at = current_timestamp
where session.revoked_at is null
  and exists (
      select 1
      from user_accounts account
      where lower(account.username) = lower(session.username)
        and account.role <> 'CUSTOMER'
        and account.operational_access_verified = false
  );

alter table user_accounts
    add constraint chk_user_accounts_provisioning_source
        check (provisioning_source in ('SELF_SERVICE', 'ADMIN_PROVISIONED', 'BOOTSTRAP', 'UNKNOWN'));

create index idx_user_accounts_security_review
    on user_accounts (operational_access_verified, role, provisioning_source);
