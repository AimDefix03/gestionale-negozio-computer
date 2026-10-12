\if :{?product_count}
\else
\echo 'product_count is required'
\quit
\endif

\if :{?partner_count}
\else
\echo 'partner_count is required'
\quit
\endif

\if :{?order_count}
\else
\echo 'order_count is required'
\quit
\endif

insert into products (
    code, name, description, category, brand, product_type, usage_context,
    quantity, price, discount, version, reserved_quantity, discontinued
)
select
    'VOL-P-' || lpad(series_id::text, 6, '0'),
    'Prodotto volume ' || series_id,
    'Record sintetico per test di volume.',
    case when series_id % 5 = 0 then 'SOFTWARE' else 'HARDWARE' end,
    'Volume Brand ' || ((series_id - 1) % 20 + 1),
    case when series_id % 5 = 0 then 'LICENSE' else 'COMPONENT' end,
    null,
    100,
    (25 + series_id % 500)::numeric(12, 2),
    (series_id % 4)::numeric(5, 2),
    0,
    case when series_id % 10 = 0 then 5 else 0 end,
    false
from generate_series(1, :product_count) as generated(series_id);

insert into business_partners (
    code, type, display_name, tax_code, vat_number, email, phone, address, city,
    notes, active, created_at, updated_at
)
select
    'VOL-C-' || lpad(series_id::text, 6, '0'),
    'CUSTOMER',
    'Cliente volume ' || series_id,
    'VOLTAX' || lpad(series_id::text, 10, '0'),
    null,
    'cliente' || series_id || '@example.invalid',
    null,
    'Via Volume ' || series_id,
    'Citta Demo',
    'Record sintetico per test di volume.',
    true,
    timestamp '2026-02-01 08:00:00' + series_id * interval '1 second',
    timestamp '2026-02-01 08:00:00' + series_id * interval '1 second'
from generate_series(1, :partner_count) as generated(series_id);

insert into customer_orders (
    code, customer, timestamp, payment_method, total, status, status_changed_at, customer_code
)
select
    'VOL-O-' || lpad(series_id::text, 8, '0'),
    'cliente.volume.' || (((series_id - 1) % :partner_count) + 1),
    timestamp '2026-02-02 08:00:00' + series_id * interval '1 second',
    case series_id % 4
        when 0 then 'Carta'
        when 1 then 'Contanti'
        when 2 then 'Bonifico bancario'
        else 'Metodo volume'
    end,
    (25 + series_id % 500)::numeric(12, 2),
    case series_id % 4
        when 0 then 'DRAFT'
        when 1 then 'CONFIRMED'
        when 2 then 'FULFILLED'
        else 'CANCELED'
    end,
    timestamp '2026-02-02 08:00:00' + series_id * interval '1 second',
    'VOL-C-' || lpad((((series_id - 1) % :partner_count) + 1)::text, 6, '0')
from generate_series(1, :order_count) as generated(series_id);

insert into order_items (
    order_id, product_code, product_name, quantity, unit_price, line_total
)
select
    customer_order.id,
    product.code,
    product.name,
    1,
    customer_order.total,
    customer_order.total
from customer_orders customer_order
join products product
    on product.code = 'VOL-P-' || lpad(
        ((((substring(customer_order.code from 7))::integer - 1) % :product_count) + 1)::text,
        6,
        '0'
    )
where customer_order.code like 'VOL-O-%';

insert into stock_movements (
    timestamp, actor, role, product_code, product_name, type, quantity,
    previous_quantity, new_quantity, reason
)
select
    timestamp '2026-02-01 07:00:00' + row_number() over (order by id) * interval '1 second',
    'fixture.volume',
    'EMPLOYEE',
    code,
    name,
    'LOAD',
    100,
    0,
    100,
    'Carico sintetico per profilo di volume.'
from products
where code like 'VOL-P-%';
