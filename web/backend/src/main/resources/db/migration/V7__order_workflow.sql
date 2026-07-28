alter table customer_orders add column status varchar(255) not null default 'FULFILLED';
alter table customer_orders add column status_changed_at timestamp(6) not null default current_timestamp;
