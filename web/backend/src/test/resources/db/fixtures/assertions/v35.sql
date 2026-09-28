do $$
declare
    invented_sessions bigint;
    invented_items bigint;
    invented_movements bigint;
    required_tables bigint;
    required_columns bigint;
    required_constraints bigint;
    required_sequences bigint;
begin
    select count(*) into invented_sessions from physical_inventory_sessions;
    select count(*) into invented_items from physical_inventory_items;
    select count(*) into invented_movements
    from stock_movements
    where physical_inventory_session_id is not null
       or physical_inventory_item_id is not null
       or origin = 'PHYSICAL_INVENTORY';

    if invented_sessions <> 0 or invented_items <> 0 or invented_movements <> 0 then
        raise exception 'V35 must not infer physical inventory sessions from historical stock';
    end if;

    select count(*) into required_tables
    from information_schema.tables
    where table_schema = 'public'
      and table_name in ('physical_inventory_sessions', 'physical_inventory_items');

    if required_tables <> 2 then
        raise exception 'V35 must install the governed physical inventory tables';
    end if;

    select count(*) into required_columns
    from information_schema.columns
    where table_schema = 'public'
      and (table_name, column_name) in (
          ('physical_inventory_sessions', 'submitted_by'),
          ('physical_inventory_sessions', 'approved_by'),
          ('physical_inventory_sessions', 'approval_reason'),
          ('physical_inventory_items', 'theoretical_quantity_at_count'),
          ('physical_inventory_items', 'difference_quantity'),
          ('physical_inventory_items', 'quantity_before_approval'),
          ('physical_inventory_items', 'compensated_movement_delta'),
          ('physical_inventory_items', 'active_marker'),
          ('stock_movements', 'physical_inventory_session_id'),
          ('stock_movements', 'physical_inventory_item_id')
      );

    if required_columns <> 10 then
        raise exception 'V35 leaves % physical inventory evidence columns missing', 10 - required_columns;
    end if;

    select count(*) into required_constraints
    from pg_constraint
    where conname in (
        'uk_physical_inventory_sessions_code',
        'chk_physical_inventory_sessions_lifecycle',
        'chk_physical_inventory_separation',
        'uk_physical_inventory_items_session_product',
        'uk_physical_inventory_items_active_product',
        'chk_physical_inventory_items_count',
        'chk_physical_inventory_items_approval',
        'uk_stock_movements_physical_inventory_item',
        'chk_stock_movements_physical_inventory'
    );

    if required_constraints <> 9 then
        raise exception 'V35 must enforce lifecycle, uniqueness, ledger linkage and approval separation';
    end if;

    select count(*) into required_sequences
    from information_schema.sequences
    where sequence_schema = 'public'
      and sequence_name = 'physical_inventory_session_code_seq';

    if required_sequences <> 1 then
        raise exception 'V35 must install the physical inventory business-code sequence';
    end if;
end $$;
