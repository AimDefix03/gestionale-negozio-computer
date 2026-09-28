insert into products (
    id, code, name, description, category, brand, product_type, usage_context,
    quantity, price, discount, version, reserved_quantity, discontinued
) values
    (1101, 'FIX-LEDGER', 'Prodotto ledger', 'Prodotto sintetico con giacenza non riconciliata.', 'HARDWARE', 'Fixture Brand', 'COMPONENT', 'TEST', 10, 100.00, 0.00, 0, 2, false),
    (1102, 'FIX-CASE', 'Prodotto collisione A', 'Prima variante sintetica del codice.', 'HARDWARE', 'Fixture Brand', 'COMPONENT', null, 8, 80.00, 5.00, 0, 0, false),
    (1103, 'fix-case', 'Prodotto collisione B', 'Seconda variante sintetica del codice.', 'HARDWARE', 'Fixture Brand', 'COMPONENT', null, 6, 60.00, 0.00, 0, 0, false),
    (1104, 'FIX-RESERVED', 'Prodotto riservato', 'Prodotto sintetico completamente riservato.', 'HARDWARE', 'Fixture Brand', 'COMPONENT', 'TEST', 3, 150.00, 0.00, 0, 3, false),
    (1105, 'FIX-ENDED', 'Prodotto dismesso', 'Prodotto sintetico mantenuto per lo storico.', 'SOFTWARE', 'Fixture Brand', 'LICENSE', null, 0, 40.00, 0.00, 0, 0, true);

insert into user_accounts (id, username, password_salt, password_hash, role) values
    (1201, 'cliente.case', 'fixture-salt-1201', 'fixture-hash-1201-not-a-credential', 'CUSTOMER'),
    (1202, 'CLIENTE.CASE', 'fixture-salt-1202', 'fixture-hash-1202-not-a-credential', 'CUSTOMER'),
    (1203, 'cliente.riusato', 'fixture-salt-1203', 'fixture-hash-1203-not-a-credential', 'CUSTOMER'),
    (1204, 'operatore.fixture', 'fixture-salt-1204', 'fixture-hash-1204-not-a-credential', 'EMPLOYEE'),
    (1205, 'amministratore.fixture', 'fixture-salt-1205', 'fixture-hash-1205-not-a-credential', 'ADMIN'),
    (1206, 'super.fixture', 'fixture-salt-1206', 'fixture-hash-1206-not-a-credential', 'SUPER_ADMIN');

insert into auth_sessions (
    id, token_hash, username, role, created_at, expires_at, revoked_at
) values
    (1211, repeat('a', 64), 'cliente.riusato', 'CUSTOMER', timestamp '2026-01-10 09:00:00', timestamp '2099-01-10 09:00:00', null),
    (1212, repeat('b', 64), 'cliente.case', 'CUSTOMER', timestamp '2026-01-10 09:05:00', timestamp '2099-01-10 09:05:00', timestamp '2026-01-10 09:10:00'),
    (1213, repeat('c', 64), 'CLIENTE.CASE', 'CUSTOMER', timestamp '2026-01-10 09:15:00', timestamp '2099-01-10 09:15:00', null),
    (1214, repeat('d', 64), 'utente.rimosso', 'CUSTOMER', timestamp '2026-01-10 09:20:00', timestamp '2099-01-10 09:20:00', null);

insert into login_attempts (
    id, username_key, attempts, first_attempt_at, last_attempt_at, locked_until
) values
    (1221, 'cliente.case', 2, timestamp '2026-01-11 10:00:00', timestamp '2026-01-11 10:02:00', null),
    (1222, 'CLIENTE.CASE', 4, timestamp '2026-01-11 10:03:00', timestamp '2026-01-11 10:07:00', timestamp '2026-01-11 10:22:00');

insert into business_partners (
    id, code, type, display_name, tax_code, vat_number, email, phone, address, city,
    notes, active, created_at, updated_at
) values
    (1301, 'FIX-CUSTOMER', 'CUSTOMER', 'Cliente Fixture Principale', 'FIXTURETAX001', null, 'cliente1@example.invalid', null, 'Via Sintetica 1', 'Citta Demo', 'Record interamente sintetico.', true, timestamp '2026-01-01 08:00:00', timestamp '2026-01-01 08:00:00'),
    (1302, 'FIX-CASE-CUSTOMER', 'CUSTOMER', 'Cliente Omonimo', 'FIXTURETAX002', null, 'cliente2@example.invalid', null, 'Via Sintetica 2', 'Citta Demo', 'Prima collisione case-insensitive.', true, timestamp '2026-01-01 08:05:00', timestamp '2026-01-01 08:05:00'),
    (1303, 'fix-case-customer', 'CUSTOMER', 'Cliente Omonimo', 'FIXTURETAX003', null, 'cliente3@example.invalid', null, 'Via Sintetica 3', 'Citta Demo', 'Seconda collisione case-insensitive.', true, timestamp '2026-01-01 08:10:00', timestamp '2026-01-01 08:10:00');

insert into customer_orders (
    id, code, customer, timestamp, payment_method, total, status, status_changed_at, customer_code
) values
    (1401, 'FIX-ORD-DRAFT', 'cliente.case', timestamp '2026-01-15 09:00:00', 'Carta', 80.00, 'DRAFT', timestamp '2026-01-15 09:00:00', 'FIX-CASE-CUSTOMER'),
    (1402, 'FIX-ORD-CONFIRMED', 'CLIENTE.CASE', timestamp '2026-01-16 10:00:00', 'Contanti', 120.00, 'CONFIRMED', timestamp '2026-01-16 10:05:00', 'fix-case-customer'),
    (1403, 'FIX-ORD-FULFILLED', 'cliente.riusato', timestamp '2026-01-17 11:00:00', 'Bonifico bancario', 200.00, 'FULFILLED', timestamp '2026-01-17 12:00:00', 'FIX-CUSTOMER'),
    (1404, 'FIX-ORD-CANCELED', 'cliente.case', timestamp '2026-01-18 12:00:00', 'Voucher storico', 60.00, 'CANCELED', timestamp '2026-01-18 12:10:00', 'FIX-CASE-CUSTOMER'),
    (1405, 'FIX-ORD-YEAR-BOUNDARY', 'cliente.riusato', timestamp '2025-12-31 23:30:00', 'CASH', 100.00, 'FULFILLED', timestamp '2026-01-01 00:05:00', 'FIX-CUSTOMER');

insert into order_items (
    id, order_id, product_code, product_name, quantity, unit_price, line_total
) values
    (1501, 1401, 'FIX-CASE', 'Prodotto collisione A', 1, 80.00, 80.00),
    (1502, 1402, 'fix-case', 'Prodotto collisione B', 2, 60.00, 120.00),
    (1503, 1403, 'FIX-LEDGER', 'Prodotto ledger', 2, 100.00, 200.00),
    (1504, 1404, 'fix-case', 'Prodotto collisione B', 1, 60.00, 60.00),
    (1505, 1405, 'FIX-LEDGER', 'Prodotto ledger', 1, 100.00, 100.00);

insert into stock_movements (
    id, timestamp, actor, role, product_code, product_name, type, quantity,
    previous_quantity, new_quantity, reason
) values
    (1551, timestamp '2026-01-02 08:00:00', 'operatore.fixture', 'EMPLOYEE', 'FIX-LEDGER', 'Prodotto ledger', 'LOAD', 12, 0, 12, 'Carico sintetico iniziale.'),
    (1552, timestamp '2026-01-03 08:00:00', 'operatore.fixture', 'EMPLOYEE', 'FIX-LEDGER', 'Prodotto ledger', 'UNLOAD', 1, 12, 11, 'Scarico sintetico precedente alla riconciliazione.');

insert into fiscal_documents (
    id, code, type, status, created_at, related_order_code, customer, payment_method,
    taxable_amount, vat_rate, vat_amount, total_amount, created_by, created_by_role,
    reason, disclaimer, customer_snapshot_code, customer_snapshot_name,
    customer_snapshot_tax_code, customer_snapshot_vat_number, customer_snapshot_email,
    customer_snapshot_phone, customer_snapshot_address, customer_snapshot_city
) values
    (1, 'FIX-FS-2026-0001', 'SIMULATED_INVOICE', 'ISSUED', timestamp '2026-01-17 12:05:00', 'FIX-ORD-FULFILLED', 'cliente.riusato', 'Bonifico bancario', 163.93, 0.22, 36.07, 200.00, 'operatore.fixture', 'EMPLOYEE', 'Documento sintetico storico.', 'DOCUMENTO SIMULATO NON FISCALE', 'FIX-CUSTOMER', 'Cliente Fixture Principale', 'FIXTURETAX001', '', 'cliente1@example.invalid', '', 'Via Sintetica 1', 'Citta Demo'),
    (2, 'FIX-FS-2025-0001', 'SIMULATED_INVOICE', 'ISSUED', timestamp '2025-12-31 23:45:00', 'FIX-ORD-YEAR-BOUNDARY', 'cliente.riusato', 'CASH', 81.97, 0.22, 18.03, 100.00, 'operatore.fixture', 'EMPLOYEE', 'Documento sintetico al confine anno.', 'DOCUMENTO SIMULATO NON FISCALE', 'FIX-CUSTOMER', 'Cliente Fixture Principale', 'FIXTURETAX001', '', 'cliente1@example.invalid', '', 'Via Sintetica 1', 'Citta Demo'),
    (3, 'FIX-NC-2026-0001', 'SIMULATED_CREDIT_NOTE', 'DRAFT', timestamp '2026-01-18 12:20:00', 'FIX-ORD-CANCELED', 'cliente.case', 'Voucher storico', 49.18, 0.22, 10.82, 60.00, 'amministratore.fixture', 'ADMIN', 'Documento sintetico di rettifica.', 'DOCUMENTO SIMULATO NON FISCALE', 'FIX-CASE-CUSTOMER', 'Cliente Omonimo', 'FIXTURETAX002', '', 'cliente2@example.invalid', '', 'Via Sintetica 2', 'Citta Demo');

insert into fiscal_document_lines (
    id, document_id, product_code, description, quantity, unit_price, line_total
) values
    (1701, 1, 'FIX-LEDGER', 'Prodotto ledger', 2, 81.965, 163.93),
    (1702, 2, 'FIX-LEDGER', 'Prodotto ledger', 1, 81.97, 81.97),
    (1703, 3, 'fix-case', 'Prodotto collisione B', 1, 49.18, 49.18);

insert into audit_events (
    id, timestamp, actor, role, action, target, details, category, severity,
    request_id, source, entity_type
) values
    (1801, timestamp '2026-01-15 09:00:00', 'cliente.case', 'CUSTOMER', 'ORDER_CREATED', 'FIX-ORD-DRAFT', 'Evento sintetico anonimizzato.', 'ORDERS', 'INFO', 'fixture-request-0001', 'FIXTURE', 'CUSTOMER_ORDER'),
    (1802, timestamp '2026-01-17 12:05:00', 'operatore.fixture', 'EMPLOYEE', 'DOCUMENT_CREATED', 'FIX-FS-2026-0001', 'Evento sintetico anonimizzato.', 'DOCUMENTS', 'INFO', 'fixture-request-0002', 'FIXTURE', 'FISCAL_DOCUMENT'),
    (1803, timestamp '2026-01-01 08:30:00', 'Sistema', 'Super admin', 'CREATE_ACCOUNT', 'operatore.fixture', 'Registrazione pubblica - ruolo Dipendente', 'ACCOUNT', 'WARNING', 'fixture-request-0003', 'FIXTURE', 'USER_ACCOUNT');

insert into idempotency_records (
    id, idem_key, actor, operation, request_hash, status, response_body,
    response_type, http_status, created_at, completed_at
) values
    (1901, 'fixture-completed', 'cliente.case', 'CREATE_ORDER', repeat('d', 64), 'COMPLETED', '{"orderCode":"FIX-ORD-DRAFT"}', 'application/json', 201, timestamp '2026-01-15 09:00:00', timestamp '2026-01-15 09:00:01'),
    (1902, 'fixture-in-progress', 'CLIENTE.CASE', 'CREATE_ORDER', repeat('e', 64), 'IN_PROGRESS', null, null, null, timestamp '2026-01-16 10:00:00', null);

select setval(pg_get_serial_sequence('products', 'id'), (select max(id) from products), true);
select setval(pg_get_serial_sequence('user_accounts', 'id'), (select max(id) from user_accounts), true);
select setval(pg_get_serial_sequence('auth_sessions', 'id'), (select max(id) from auth_sessions), true);
select setval(pg_get_serial_sequence('login_attempts', 'id'), (select max(id) from login_attempts), true);
select setval(pg_get_serial_sequence('business_partners', 'id'), (select max(id) from business_partners), true);
select setval(pg_get_serial_sequence('customer_orders', 'id'), (select max(id) from customer_orders), true);
select setval(pg_get_serial_sequence('order_items', 'id'), (select max(id) from order_items), true);
select setval(pg_get_serial_sequence('stock_movements', 'id'), (select max(id) from stock_movements), true);
select setval(pg_get_serial_sequence('fiscal_documents', 'id'), (select max(id) from fiscal_documents), true);
select setval(pg_get_serial_sequence('fiscal_document_lines', 'id'), (select max(id) from fiscal_document_lines), true);
select setval(pg_get_serial_sequence('audit_events', 'id'), (select max(id) from audit_events), true);
select setval(pg_get_serial_sequence('idempotency_records', 'id'), (select max(id) from idempotency_records), true);
