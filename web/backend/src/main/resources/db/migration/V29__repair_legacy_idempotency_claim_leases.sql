update idempotency_records
set claim_token = coalesce(claim_token, 'legacy-recovered-' || cast(id as varchar)),
    lease_expires_at = coalesce(lease_expires_at, created_at),
    updated_at = greatest(updated_at, created_at)
where status = 'IN_PROGRESS'
  and (claim_token is null or lease_expires_at is null);

alter table idempotency_records
    add constraint chk_idempotency_in_progress_claim check (
        status <> 'IN_PROGRESS'
        or (
            claim_token is not null
            and char_length(trim(claim_token)) > 0
            and lease_expires_at is not null
        )
    );
