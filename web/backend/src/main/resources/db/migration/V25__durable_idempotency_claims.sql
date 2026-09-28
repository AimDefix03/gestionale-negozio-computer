alter table idempotency_records add column actor_account_id bigint;
alter table idempotency_records add column fingerprint_version integer not null default 1;
alter table idempotency_records add column claim_token varchar(64);
alter table idempotency_records add column lease_expires_at timestamp(6);
alter table idempotency_records add column expires_at timestamp(6);
alter table idempotency_records add column updated_at timestamp(6);
alter table idempotency_records add column failed_at timestamp(6);

update idempotency_records record
set actor_account_id = coalesce(
        (
            select min(account.id)
            from user_accounts account
            where lower(account.username) = lower(record.actor)
        ),
        -record.id
    ),
    claim_token = case
        when record.status = 'PROCESSING' then 'legacy-' || record.id
        else null
    end,
    lease_expires_at = case
        when record.status = 'PROCESSING' then record.created_at
        else null
    end,
    expires_at = coalesce(record.completed_at, record.created_at) + interval '24' hour,
    updated_at = coalesce(record.completed_at, record.created_at);

update idempotency_records
set status = 'IN_PROGRESS'
where status = 'PROCESSING';

alter table idempotency_records alter column actor_account_id set not null;
alter table idempotency_records alter column expires_at set not null;
alter table idempotency_records alter column updated_at set not null;

alter table idempotency_records drop constraint uk_idempotency_actor_operation_key;

alter table idempotency_records
    add constraint uk_idempotency_account_operation_key
        unique (actor_account_id, operation, idem_key);

alter table idempotency_records
    add constraint chk_idempotency_status
        check (status in ('IN_PROGRESS', 'COMPLETED', 'FAILED_RETRYABLE'));

create index idx_idempotency_expiry on idempotency_records (expires_at, status, lease_expires_at);
create index idx_idempotency_account_operation on idempotency_records (actor_account_id, operation);
