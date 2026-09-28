do $$
declare
    ambiguous_status varchar(40);
    evidenced_status varchar(40);
    evidence_count bigint;
    canceled_status varchar(40);
    invoice_next bigint;
    credit_note_next bigint;
    lagging_counters bigint;
begin
    select payment.status into ambiguous_status
    from order_payments payment
    join customer_orders customer_order on customer_order.id = payment.order_id
    where customer_order.code = 'FIX-ORD-DRAFT';

    if ambiguous_status <> 'UNRECONCILED' then
        raise exception 'V22 must quarantine payment history without evidence as UNRECONCILED';
    end if;

    select payment.status into evidenced_status
    from order_payments payment
    join customer_orders customer_order on customer_order.id = payment.order_id
    where customer_order.code = 'FIX-ORD-CONFIRMED';

    select count(*) into evidence_count
    from payment_transactions transaction
    join order_payments payment on payment.id = transaction.payment_id
    join customer_orders customer_order on customer_order.id = payment.order_id
    where customer_order.code = 'FIX-ORD-CONFIRMED';

    if evidence_count > 0 and evidenced_status <> 'PAID' then
        raise exception 'V22 must preserve payment states backed by transactions';
    end if;

    if evidence_count = 0 and evidenced_status <> 'UNRECONCILED' then
        raise exception 'V22 must not infer an unpaid state when historical evidence is absent';
    end if;

    select payment.status into canceled_status
    from order_payments payment
    join customer_orders customer_order on customer_order.id = payment.order_id
    where customer_order.code = 'FIX-ORD-CANCELED';

    if canceled_status <> 'CANCELED' then
        raise exception 'V22 must preserve canceled payments';
    end if;

    select next_value into invoice_next
    from document_number_counters
    where document_type = 'SIMULATED_INVOICE' and fiscal_year = 2026;

    select next_value into credit_note_next
    from document_number_counters
    where document_type = 'SIMULATED_CREDIT_NOTE' and fiscal_year = 2026;

    if invoice_next <> 2 or credit_note_next <> 4 then
        raise exception 'V22 counters must continue from historical max plus one: invoice %, credit note %', invoice_next, credit_note_next;
    end if;

    select count(*) into lagging_counters
    from document_number_counters counter
    join (
        select type, fiscal_year, max(sequence_number) as maximum_sequence
        from fiscal_documents
        group by type, fiscal_year
    ) history on history.type = counter.document_type and history.fiscal_year = counter.fiscal_year
    where counter.next_value <= history.maximum_sequence;

    if lagging_counters <> 0 then
        raise exception 'V22 leaves % document counters behind historical numbering', lagging_counters;
    end if;

    if not exists (
        select 1 from information_schema.columns
        where table_schema = 'public' and table_name = 'order_payments' and column_name = 'reconciliation_reason'
    ) then
        raise exception 'V22 reconciliation evidence columns are missing';
    end if;
end $$;
