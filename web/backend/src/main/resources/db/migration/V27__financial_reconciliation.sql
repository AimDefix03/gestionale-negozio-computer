alter table payment_transactions add column return_id bigint;
alter table payment_transactions add column reconciliation_payment_id bigint;
alter table payment_transactions drop constraint chk_payment_transactions_type;

update payment_transactions
set reference = null
where reference is not null and char_length(trim(reference)) = 0;

update payment_transactions
set return_code = null
where return_code is not null and char_length(trim(return_code)) = 0;

update payment_transactions
set return_id = (
    select order_returns.id
    from order_returns
    join order_payments on order_payments.order_id = order_returns.order_id
    where order_payments.id = payment_transactions.payment_id
      and lower(trim(order_returns.code)) = lower(trim(payment_transactions.return_code))
)
where type = 'REFUND';

insert into payment_transactions (
    code,
    payment_id,
    type,
    amount,
    reference,
    reason,
    return_code,
    cancellation_order_id,
    return_id,
    reconciliation_payment_id,
    recorded_at,
    recorded_by,
    recorded_by_role
)
select
    'MIG-V27-REC-' || order_payments.id,
    order_payments.id,
    'RECONCILIATION',
    order_payments.paid_amount,
    nullif(trim(order_payments.reconciliation_reference), ''),
    order_payments.reconciliation_reason,
    null,
    null,
    null,
    order_payments.id,
    order_payments.reconciled_at,
    order_payments.reconciled_by,
    order_payments.reconciled_by_role
from order_payments
where order_payments.reconciled_at is not null
  and order_payments.paid_amount > 0
  and not exists (
      select 1
      from payment_transactions
      where payment_transactions.payment_id = order_payments.id
  );

alter table payment_transactions drop constraint chk_payment_transactions_cancellation_link;
alter table payment_transactions add constraint chk_payment_transactions_type check (type in ('RECEIPT', 'REFUND', 'REVERSAL', 'RECONCILIATION'));
alter table payment_transactions add constraint fk_payment_transactions_return foreign key (return_id) references order_returns (id);
alter table payment_transactions add constraint fk_payment_transactions_reconciliation_payment foreign key (reconciliation_payment_id) references order_payments (id);
alter table payment_transactions add constraint uk_payment_transactions_reconciliation_payment unique (reconciliation_payment_id);
alter table payment_transactions add constraint chk_payment_transactions_financial_link check (
    (type = 'RECEIPT'
        and return_code is null
        and return_id is null
        and cancellation_order_id is null
        and reconciliation_payment_id is null)
    or (type = 'REFUND'
        and return_code is not null
        and char_length(trim(return_code)) > 0
        and return_id is not null
        and cancellation_order_id is null
        and reconciliation_payment_id is null)
    or (type = 'REVERSAL'
        and return_code is null
        and return_id is null
        and cancellation_order_id is not null
        and reconciliation_payment_id is null
        and reference is not null
        and char_length(trim(reference)) > 0)
    or (type = 'RECONCILIATION'
        and return_code is null
        and return_id is null
        and cancellation_order_id is null
        and reconciliation_payment_id = payment_id)
);

alter table order_payments add constraint chk_order_payments_timeline check (
    updated_at >= created_at
    and (reconciled_at is null or reconciled_at >= created_at and reconciled_at <= updated_at)
);

alter table order_returns add constraint chk_order_returns_timeline check (
    updated_at >= requested_at
    and (reviewed_at is null or reviewed_at >= requested_at and reviewed_at <= updated_at)
    and (received_at is null or received_at >= requested_at and received_at <= updated_at)
);

alter table order_returns add constraint chk_order_returns_lifecycle_metadata check (
    (status = 'REQUESTED' and reviewed_at is null and received_at is null)
    or (status in ('APPROVED', 'REJECTED') and reviewed_at is not null and received_at is null)
    or (status in ('RECEIVED', 'PARTIALLY_REFUNDED', 'REFUNDED') and reviewed_at is not null and received_at is not null and received_at >= reviewed_at)
);

create index idx_payment_transactions_return_id on payment_transactions (return_id, recorded_at);
