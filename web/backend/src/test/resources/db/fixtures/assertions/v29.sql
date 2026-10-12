do $$
declare
    canonical_collisions bigint;
    missing_baselines bigint;
    invalid_ledger_chains bigint;
    invalid_idempotency bigint;
    invalid_cancellations bigint;
    invalid_financial_links bigint;
    financial_mismatches bigint;
    merged_attempts integer;
begin
    select
        (select count(*) from (select lower(trim(username)) from user_accounts group by lower(trim(username)) having count(*) > 1) duplicate_users)
      + (select count(*) from (select lower(trim(code)) from products group by lower(trim(code)) having count(*) > 1) duplicate_products)
      + (select count(*) from (select lower(trim(code)) from business_partners group by lower(trim(code)) having count(*) > 1) duplicate_partners)
    into canonical_collisions;

    if canonical_collisions <> 0 then
        raise exception 'V29 leaves % canonical business identifier collisions', canonical_collisions;
    end if;

    if not exists (select 1 from products where id = 1102 and code = 'FIX-CASE' and quantity = 14)
       or exists (select 1 from products where id = 1103)
       or exists (select 1 from user_accounts where id = 1202)
       or exists (select 1 from business_partners where id = 1303) then
        raise exception 'Approved pre-V23 canonical remediation was not preserved';
    end if;

    if not exists (select 1 from auth_sessions where id = 1213 and account_id = 1201 and username_snapshot = 'CLIENTE.CASE')
       or not exists (select 1 from customer_orders where id = 1402 and partner_id = 1302 and customer_code = 'fix-case-customer') then
        raise exception 'Stable references or immutable business snapshots were lost during canonical remediation';
    end if;

    select count(*) into missing_baselines
    from products product
    where (select count(*) from stock_movements movement where movement.product_id = product.id and movement.authoritative and movement.baseline_marker = true) <> 1;

    if missing_baselines <> 0 then
        raise exception 'V29 leaves % products without exactly one authoritative inventory baseline', missing_baselines;
    end if;

    select count(*) into invalid_ledger_chains
    from stock_movements
    where authoritative
      and (product_id is null or new_quantity <> previous_quantity + delta_quantity);

    if invalid_ledger_chains <> 0 then
        raise exception 'V29 leaves % invalid authoritative inventory movements', invalid_ledger_chains;
    end if;

    select count(*) into invalid_idempotency
    from idempotency_records
    where actor_account_id is null
       or fingerprint_version <= 0
       or expires_at is null
       or updated_at is null
       or status not in ('IN_PROGRESS', 'COMPLETED', 'FAILED_RETRYABLE')
       or (status = 'IN_PROGRESS' and (claim_token is null or lease_expires_at is null));

    if invalid_idempotency <> 0 then
        raise exception 'V29 leaves % invalid durable idempotency claims', invalid_idempotency;
    end if;

    select count(*) into invalid_cancellations
    from customer_orders
    where (status = 'CANCELED' and (cancellation_reason is null or canceled_at is null or canceled_by is null or canceled_by_role is null))
       or (status <> 'CANCELED' and (cancellation_reference is not null or cancellation_reason is not null or canceled_at is not null or canceled_by is not null or canceled_by_role is not null));

    if invalid_cancellations <> 0 then
        raise exception 'V29 leaves % orders with inconsistent cancellation metadata', invalid_cancellations;
    end if;

    select count(*) into invalid_financial_links
    from payment_transactions transaction
    where (transaction.type = 'REFUND' and transaction.return_id is null)
       or (transaction.type = 'REVERSAL' and transaction.cancellation_order_id is null)
       or (transaction.type = 'RECONCILIATION' and transaction.reconciliation_payment_id is distinct from transaction.payment_id);

    if invalid_financial_links <> 0 then
        raise exception 'V29 leaves % financial transactions without stable domain links', invalid_financial_links;
    end if;

    select count(*) into financial_mismatches
    from order_payments payment
    join customer_orders customer_order on customer_order.id = payment.order_id
    where customer_order.code = 'FIX-ORD-YEAR-BOUNDARY'
      and payment.paid_amount <> coalesce((
          select sum(case when transaction.type in ('RECEIPT', 'RECONCILIATION') then transaction.amount else -transaction.amount end)
          from payment_transactions transaction
          where transaction.payment_id = payment.id
      ), 0);

    if exists (select 1 from payment_transactions where code = 'FIX-PAY-MISMATCH')
       and financial_mismatches <> 1 then
        raise exception 'V29 must retain and detect the intentional financial reconciliation mismatch';
    end if;

    if not exists (select 1 from payment_transactions where code = 'FIX-PAY-MISMATCH')
       and financial_mismatches <> 0 then
        raise exception 'V29 reported a financial mismatch absent from the source fixture';
    end if;

    select attempts into merged_attempts
    from login_attempts
    where username_key = 'cliente.case';

    if merged_attempts <> 6
       or (select count(*) from login_attempts where lower(trim(username_key)) = 'cliente.case') <> 1
       or exists (select 1 from login_attempts where username_key <> lower(trim(username_key))) then
        raise exception 'V29 did not merge concurrent login-attempt history deterministically';
    end if;
end $$;
