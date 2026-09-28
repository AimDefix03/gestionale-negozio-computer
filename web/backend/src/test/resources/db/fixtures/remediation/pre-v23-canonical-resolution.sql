update auth_sessions
set account_id = 1201
where account_id = 1202;

update business_partners
set linked_account_id = 1201
where linked_account_id = 1202;

update customer_orders
set customer_account_id = 1201
where customer_account_id = 1202;

delete from user_accounts
where id = 1202;

update customer_orders
set partner_id = 1302
where partner_id = 1303;

delete from business_partners
where id = 1303;

update products keeper
set quantity = keeper.quantity + duplicate.quantity,
    reserved_quantity = keeper.reserved_quantity + duplicate.reserved_quantity,
    version = greatest(keeper.version, duplicate.version) + 1
from products duplicate
where keeper.id = 1102
  and duplicate.id = 1103;

delete from products
where id = 1103;
