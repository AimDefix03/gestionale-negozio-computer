do $$
begin
    if exists (
        select 1
        from customer_orders
        where customer_account_id is not null
    ) then
        raise exception 'V21 inferred account ownership from legacy textual data';
    end if;

    if exists (
        select 1
        from customer_orders customer_order
        join business_partners partner on partner.code = customer_order.customer_code
        where customer_order.partner_id is distinct from partner.id
           or customer_order.ownership_status <> 'PARTNER'
    ) then
        raise exception 'V21 did not migrate an exact partner-code ownership deterministically';
    end if;

    if not exists (
        select 1
        from customer_orders
        where id = 1401
          and customer = 'cliente.case'
          and customer_account_id is null
          and partner_id = 1302
          and ownership_status = 'PARTNER'
    ) then
        raise exception 'V21 changed the customer snapshot or missed the exact partner link';
    end if;

    if not exists (
        select 1
        from customer_orders
        where id = 1402
          and customer = 'CLIENTE.CASE'
          and customer_account_id is null
          and partner_id = 1303
          and ownership_status = 'PARTNER'
    ) then
        raise exception 'V21 collapsed case-distinct customer snapshots or partner codes';
    end if;

    if exists (
        select 1
        from business_partners
        where linked_account_id is not null
    ) then
        raise exception 'V21 inferred account-partner links without explicit verification';
    end if;
end $$;
