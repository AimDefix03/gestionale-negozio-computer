alter table order_payments drop constraint chk_order_payments_status;

alter table order_payments
    add constraint chk_order_payments_status check (status in ('UNRECONCILED', 'PENDING', 'PARTIALLY_PAID', 'PAID', 'FAILED', 'CANCELED', 'PARTIALLY_REFUNDED', 'REFUNDED'));

alter table order_payments add column reconciled_at timestamp(6);
alter table order_payments add column reconciled_by varchar(120);
alter table order_payments add column reconciled_by_role varchar(80);
alter table order_payments add column reconciliation_reference varchar(120);
alter table order_payments add column reconciliation_reason varchar(500);

alter table order_payments
    add constraint chk_order_payments_reconciliation_metadata check (
        (reconciled_at is null and reconciled_by is null and reconciled_by_role is null and reconciliation_reason is null)
        or
        (reconciled_at is not null and char_length(trim(reconciled_by)) > 0 and char_length(trim(reconciled_by_role)) > 0 and char_length(trim(reconciliation_reason)) > 0)
    );

update order_payments
set status = 'UNRECONCILED',
    updated_at = current_timestamp,
    version = version + 1
where status = 'PENDING'
  and paid_amount = 0
  and refunded_amount = 0
  and not exists (
      select 1
      from payment_transactions
      where payment_transactions.payment_id = order_payments.id
  );

insert into document_number_counters (document_type, fiscal_year, next_value, version)
select fiscal_documents.type, fiscal_documents.fiscal_year, max(fiscal_documents.sequence_number) + 1, 0
from fiscal_documents
where not exists (
    select 1
    from document_number_counters
    where document_number_counters.document_type = fiscal_documents.type
      and document_number_counters.fiscal_year = fiscal_documents.fiscal_year
)
group by fiscal_documents.type, fiscal_documents.fiscal_year;

update document_number_counters
set next_value = (
        select max(fiscal_documents.sequence_number) + 1
        from fiscal_documents
        where fiscal_documents.type = document_number_counters.document_type
          and fiscal_documents.fiscal_year = document_number_counters.fiscal_year
    ),
    version = version + 1
where next_value < (
    select max(fiscal_documents.sequence_number) + 1
    from fiscal_documents
    where fiscal_documents.type = document_number_counters.document_type
      and fiscal_documents.fiscal_year = document_number_counters.fiscal_year
);

create index idx_order_payments_reconciliation on order_payments (status, reconciled_at);
