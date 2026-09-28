do $fixture$
begin
    if (select count(distinct status) from customer_orders where code like 'FIX-ORD-%') <> 4 then
        raise exception 'V14 fixture must contain every order status';
    end if;
    if not exists (
        select 1 from products group by lower(code) having count(*) > 1
    ) then
        raise exception 'V14 fixture must contain a case-insensitive product collision';
    end if;
    if not exists (
        select 1 from user_accounts group by lower(username) having count(*) > 1
    ) then
        raise exception 'V14 fixture must contain a case-insensitive username collision';
    end if;
    if not exists (
        select 1 from business_partners group by lower(code) having count(*) > 1
    ) then
        raise exception 'V14 fixture must contain a case-insensitive partner collision';
    end if;
    if (select quantity from products where code = 'FIX-LEDGER') =
       (select new_quantity from stock_movements where product_code = 'FIX-LEDGER' order by timestamp desc limit 1) then
        raise exception 'V14 fixture must preserve inventory drift';
    end if;
    if (select count(*) from fiscal_documents where code like 'FIX-%') < 3 then
        raise exception 'V14 fixture must contain historical documents';
    end if;
    if not exists (
        select 1 from idempotency_records where status = 'IN_PROGRESS' and completed_at is null
    ) then
        raise exception 'V14 fixture must contain an incomplete idempotency record';
    end if;
end
$fixture$;
