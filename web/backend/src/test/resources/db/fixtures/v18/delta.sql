update company_settings
set legal_name = 'Impresa Fixture Srl',
    tax_code = 'FIXTURETAXCOMPANY',
    vat_number = '00000000000',
    email = 'azienda@example.invalid',
    phone = '+0000000000',
    address = 'Via Sintetica 100',
    postal_code = '00000',
    city = 'Citta Demo',
    province = 'XX',
    country_code = 'IT',
    updated_at = timestamp '2026-01-20 08:00:00',
    updated_by = 'super.fixture',
    version = 1
where id = 1;

insert into auth_sessions (
    id, token_hash, username, role, created_at, expires_at, revoked_at, last_used_at
) values
    (2401, repeat('f', 64), 'operatore.fixture', 'EMPLOYEE', timestamp '2026-01-20 09:00:00', timestamp '2099-01-20 09:00:00', null, timestamp '2026-01-20 09:30:00');

select setval(pg_get_serial_sequence('auth_sessions', 'id'), (select max(id) from auth_sessions), true);
