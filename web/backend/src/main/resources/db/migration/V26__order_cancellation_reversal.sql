alter table customer_orders add column cancellation_reference varchar(120);
alter table customer_orders add column cancellation_reason varchar(500);
alter table customer_orders add column canceled_at timestamp(6);
alter table customer_orders add column canceled_by varchar(120);
alter table customer_orders add column canceled_by_role varchar(80);

update customer_orders
set cancellation_reason = 'Annullamento storico antecedente alla tracciatura dettagliata',
    canceled_at = status_changed_at,
    canceled_by = 'migration-v26',
    canceled_by_role = 'SYSTEM'
where status = 'CANCELED';

alter table customer_orders add constraint chk_customer_orders_cancellation_metadata check (
    (status = 'CANCELED' and (
        cancellation_reason is not null
        and char_length(trim(cancellation_reason)) > 0
        and canceled_at is not null
        and canceled_by is not null
        and char_length(trim(canceled_by)) > 0
        and canceled_by_role is not null
        and char_length(trim(canceled_by_role)) > 0
    )) or (status <> 'CANCELED'
        and cancellation_reference is null
        and cancellation_reason is null
        and canceled_at is null
        and canceled_by is null
        and canceled_by_role is null
    )
);

alter table payment_transactions add column cancellation_order_id bigint;
alter table payment_transactions drop constraint chk_payment_transactions_type;
alter table payment_transactions add constraint chk_payment_transactions_type check (type in ('RECEIPT', 'REFUND', 'REVERSAL'));
alter table payment_transactions add constraint fk_payment_transactions_cancellation_order foreign key (cancellation_order_id) references customer_orders (id);
alter table payment_transactions add constraint uk_payment_transactions_cancellation_order unique (cancellation_order_id);
alter table payment_transactions add constraint chk_payment_transactions_cancellation_link check (
    (type = 'REVERSAL' and cancellation_order_id is not null and reference is not null and char_length(trim(reference)) > 0)
    or (type <> 'REVERSAL' and cancellation_order_id is null)
);

create index idx_payment_transactions_cancellation_order on payment_transactions (cancellation_order_id);
