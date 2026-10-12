do $fixture$
begin
    if (select count(*) from order_payments) <> (select count(*) from customer_orders) then
        raise exception 'V16 fixture requires one structured payment per order';
    end if;
    if not exists (
        select 1
        from customer_orders customer_order
        join order_payments payment on payment.order_id = customer_order.id
        where customer_order.status = 'CONFIRMED' and payment.status = 'PAID'
    ) then
        raise exception 'V16 fixture must contain a paid unfulfilled order';
    end if;
    if (select count(*) from order_returns where code like 'FIX-RET-%') < 3 then
        raise exception 'V16 fixture must contain return workflow states';
    end if;
    if not exists (
        select 1
        from order_payments payment
        left join payment_transactions payment_tx on payment_tx.payment_id = payment.id
        group by payment.id, payment.paid_amount, payment.refunded_amount
        having payment.paid_amount - payment.refunded_amount <>
               coalesce(sum(case when payment_tx.type = 'RECEIPT' then payment_tx.amount else -payment_tx.amount end), 0)
    ) then
        raise exception 'V16 fixture must contain a financial reconciliation mismatch';
    end if;
end
$fixture$;
