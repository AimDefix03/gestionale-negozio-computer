alter table business_partners
    add column linked_account_id bigint;

alter table customer_orders
    add column customer_account_id bigint;

alter table customer_orders
    add column partner_id bigint;

alter table customer_orders
    add column ownership_status varchar(32) not null default 'UNRESOLVED';

update customer_orders customer_order
set partner_id = (
    select partner.id
    from business_partners partner
    where partner.code = customer_order.customer_code
)
where customer_order.customer_code is not null
  and trim(customer_order.customer_code) <> ''
  and (
      select count(*)
      from business_partners partner
      where partner.code = customer_order.customer_code
  ) = 1;

update customer_orders
set ownership_status = 'PARTNER'
where partner_id is not null;

alter table business_partners
    add constraint fk_business_partners_linked_account
        foreign key (linked_account_id)
        references user_accounts (id)
        on delete set null;

alter table business_partners
    add constraint uk_business_partners_linked_account
        unique (linked_account_id);

alter table customer_orders
    add constraint fk_customer_orders_customer_account
        foreign key (customer_account_id)
        references user_accounts (id)
        on delete set null;

alter table customer_orders
    add constraint fk_customer_orders_partner
        foreign key (partner_id)
        references business_partners (id)
        on delete set null;

alter table customer_orders
    add constraint chk_customer_orders_ownership_status
        check (ownership_status in ('ACCOUNT', 'PARTNER', 'UNRESOLVED'));

create index idx_customer_orders_customer_account_id
    on customer_orders (customer_account_id);

create index idx_customer_orders_partner_id
    on customer_orders (partner_id);

create index idx_customer_orders_ownership_status
    on customer_orders (ownership_status);
