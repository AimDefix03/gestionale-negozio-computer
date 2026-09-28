alter table user_accounts add column username_canonical varchar(255);
alter table products add column code_canonical varchar(255);
alter table business_partners add column code_canonical varchar(255);

update user_accounts set username_canonical = lower(trim(username));
update products set code_canonical = lower(trim(code));
update business_partners set code_canonical = lower(trim(code));

alter table user_accounts alter column username_canonical set not null;
alter table products alter column code_canonical set not null;
alter table business_partners alter column code_canonical set not null;

alter table user_accounts
    add constraint chk_user_accounts_username_canonical
        check (char_length(username_canonical) > 0 and username_canonical = lower(trim(username)));

alter table products
    add constraint chk_products_code_canonical
        check (char_length(code_canonical) > 0 and code_canonical = lower(trim(code)));

alter table business_partners
    add constraint chk_business_partners_code_canonical
        check (char_length(code_canonical) > 0 and code_canonical = lower(trim(code)));

alter table user_accounts
    add constraint uk_user_accounts_username_canonical unique (username_canonical);

alter table products
    add constraint uk_products_code_canonical unique (code_canonical);

alter table business_partners
    add constraint uk_business_partners_code_canonical unique (code_canonical);
