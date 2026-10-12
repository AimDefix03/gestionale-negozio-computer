do $$
declare
    invalid_customer_types bigint;
    inferred_historical_types bigint;
    invalid_line_snapshots bigint;
begin
    select count(*) into invalid_customer_types
    from customer_orders
    where customer_type not in ('SELF_SERVICE', 'REGISTERED', 'WALK_IN', 'LEGACY_UNRESOLVED')
       or customer_type is null;

    if invalid_customer_types <> 0 then
        raise exception 'V30 leaves % orders with invalid customer classification', invalid_customer_types;
    end if;

    select count(*) into inferred_historical_types
    from customer_orders
    where customer_type <> 'LEGACY_UNRESOLVED';

    if inferred_historical_types <> 0 then
        raise exception 'V30 must not infer customer channel for % historical orders without evidence', inferred_historical_types;
    end if;

    select count(*) into invalid_line_snapshots
    from order_items
    where product_description is null;

    if invalid_line_snapshots <> 0 then
        raise exception 'V30 leaves % order lines without a description snapshot field', invalid_line_snapshots;
    end if;
end $$;
