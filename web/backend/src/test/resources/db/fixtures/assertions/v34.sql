do $$
declare
    inferred_product_costs bigint;
    invalid_legacy_receipts bigint;
    required_constraints bigint;
    required_columns bigint;
begin
    select count(*) into inferred_product_costs
    from products
    where costed_quantity <> 0
       or last_purchase_cost is not null
       or average_purchase_cost is not null;

    if inferred_product_costs <> 0 then
        raise exception 'V34 must not infer inventory cost from historical stock or selling prices';
    end if;

    select count(*) into invalid_legacy_receipts
    from supplier_order_receipt_items
    where inventory_posting_status <> 'LEGACY_UNPOSTED'
       or stock_movement_id is not null;

    if invalid_legacy_receipts <> 0 then
        raise exception 'V34 must preserve historical receipts as legacy unposted evidence';
    end if;

    select count(*) into required_columns
    from information_schema.columns
    where table_schema = 'public'
      and (table_name, column_name) in (
          ('products', 'last_purchase_cost'),
          ('products', 'average_purchase_cost'),
          ('products', 'costed_quantity'),
          ('supplier_order_receipt_items', 'actual_unit_cost'),
          ('supplier_order_receipt_items', 'inventory_posting_status'),
          ('supplier_order_receipt_items', 'stock_movement_id'),
          ('stock_movements', 'supplier_order_receipt_item_id'),
          ('stock_movements', 'unit_cost'),
          ('stock_movements', 'average_cost_after'),
          ('stock_movements', 'costed_quantity_after')
      );

    if required_columns <> 10 then
        raise exception 'V34 leaves % purchase-receipt cost columns missing', 10 - required_columns;
    end if;

    select count(*) into required_constraints
    from pg_constraint
    where conname in (
        'chk_products_purchase_costs',
        'uk_stock_movements_supplier_receipt_item',
        'chk_stock_movements_purchase_cost',
        'uk_supplier_receipt_items_stock_movement',
        'chk_supplier_receipt_items_cost',
        'chk_supplier_receipt_items_posting'
    );

    if required_constraints <> 6 then
        raise exception 'V34 must install all inventory-cost and receipt-posting constraints';
    end if;
end $$;
