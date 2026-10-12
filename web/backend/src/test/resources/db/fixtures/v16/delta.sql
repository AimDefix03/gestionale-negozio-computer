update order_payments
set status = 'PAID',
    paid_amount = requested_amount,
    updated_at = timestamp '2026-01-16 10:06:00'
where order_id = 1402;

update order_payments
set status = 'PARTIALLY_REFUNDED',
    paid_amount = requested_amount,
    refunded_amount = 50.00,
    updated_at = timestamp '2026-01-20 10:00:00'
where order_id = 1403;

update order_payments
set status = 'PARTIALLY_PAID',
    paid_amount = 40.00,
    updated_at = timestamp '2026-01-01 00:10:00'
where order_id = 1405;

insert into payment_transactions (
    id, code, payment_id, type, amount, reference, reason, return_code,
    recorded_at, recorded_by, recorded_by_role
) values
    (2101, 'FIX-PAY-0001', (select id from order_payments where order_id = 1402), 'RECEIPT', 120.00, 'FIXTURE-RECEIPT-1', 'Incasso sintetico ordine confermato.', null, timestamp '2026-01-16 10:06:00', 'operatore.fixture', 'EMPLOYEE'),
    (2102, 'FIX-PAY-0002', (select id from order_payments where order_id = 1403), 'RECEIPT', 200.00, 'FIXTURE-RECEIPT-2', 'Incasso sintetico ordine evaso.', null, timestamp '2026-01-17 12:01:00', 'operatore.fixture', 'EMPLOYEE'),
    (2103, 'FIX-REF-0001', (select id from order_payments where order_id = 1403), 'REFUND', 50.00, 'FIXTURE-REFUND-1', 'Rimborso sintetico parziale.', 'FIX-RET-0001', timestamp '2026-01-20 10:00:00', 'amministratore.fixture', 'ADMIN'),
    (2104, 'FIX-PAY-MISMATCH', (select id from order_payments where order_id = 1405), 'RECEIPT', 30.00, 'FIXTURE-MISMATCH', 'Importo intenzionalmente non riconciliato.', null, timestamp '2026-01-01 00:10:00', 'operatore.fixture', 'EMPLOYEE');

insert into order_returns (
    id, code, order_id, status, reason, total_amount, refunded_amount,
    requested_at, requested_by, requested_by_role, reviewed_at, reviewed_by,
    review_note, received_at, received_by, updated_at, version
) values
    (2201, 'FIX-RET-0001', 1403, 'PARTIALLY_REFUNDED', 'Reso sintetico parziale.', 100.00, 50.00, timestamp '2026-01-18 09:00:00', 'cliente.riusato', 'CUSTOMER', timestamp '2026-01-18 10:00:00', 'amministratore.fixture', 'Approvato per scenario fixture.', timestamp '2026-01-19 09:00:00', 'operatore.fixture', timestamp '2026-01-20 10:00:00', 0),
    (2202, 'FIX-RET-0002', 1405, 'REQUESTED', 'Reso sintetico in attesa.', 100.00, 0.00, timestamp '2026-01-02 09:00:00', 'cliente.riusato', 'CUSTOMER', null, null, null, null, null, timestamp '2026-01-02 09:00:00', 0),
    (2203, 'FIX-RET-0003', 1403, 'REJECTED', 'Reso sintetico rifiutato.', 100.00, 0.00, timestamp '2026-01-21 09:00:00', 'cliente.riusato', 'CUSTOMER', timestamp '2026-01-21 10:00:00', 'amministratore.fixture', 'Rifiutato per scenario fixture.', null, null, timestamp '2026-01-21 10:00:00', 0);

insert into order_return_items (
    id, return_id, product_code, product_name, quantity, unit_price, line_total
) values
    (2301, 2201, 'FIX-LEDGER', 'Prodotto ledger', 1, 100.00, 100.00),
    (2302, 2202, 'FIX-LEDGER', 'Prodotto ledger', 1, 100.00, 100.00),
    (2303, 2203, 'FIX-LEDGER', 'Prodotto ledger', 1, 100.00, 100.00);

select setval(pg_get_serial_sequence('payment_transactions', 'id'), (select max(id) from payment_transactions), true);
select setval(pg_get_serial_sequence('order_returns', 'id'), (select max(id) from order_returns), true);
select setval(pg_get_serial_sequence('order_return_items', 'id'), (select max(id) from order_return_items), true);
