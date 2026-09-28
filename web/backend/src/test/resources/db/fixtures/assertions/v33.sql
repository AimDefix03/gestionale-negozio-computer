do $$
declare
    missing_relations bigint;
    populated_purchase_rows bigint;
    required_constraints bigint;
begin
    select count(*) into missing_relations
    from unnest(array[
        'supplier_orders',
        'supplier_order_items',
        'supplier_order_receipts',
        'supplier_order_receipt_items',
        'supplier_order_code_seq',
        'supplier_receipt_code_seq'
    ]) relation_name
    where to_regclass(relation_name) is null;

    if missing_relations <> 0 then
        raise exception 'V33 leaves % supplier-order relations missing', missing_relations;
    end if;

    select
        (select count(*) from supplier_orders)
        + (select count(*) from supplier_order_items)
        + (select count(*) from supplier_order_receipts)
        + (select count(*) from supplier_order_receipt_items)
    into populated_purchase_rows;

    if populated_purchase_rows <> 0 then
        raise exception 'V33 must not infer supplier orders from historical sales or inventory data';
    end if;

    select count(*) into required_constraints
    from pg_constraint
    where conname in (
        'uk_supplier_orders_code',
        'chk_supplier_orders_status',
        'chk_supplier_orders_cancellation',
        'chk_supplier_order_items_quantities',
        'uk_supplier_order_items_product',
        'uk_supplier_order_receipts_code',
        'uk_supplier_order_receipt_items_line'
    );

    if required_constraints <> 7 then
        raise exception 'V33 must install all supplier-order identity, state and quantity constraints';
    end if;
end $$;
