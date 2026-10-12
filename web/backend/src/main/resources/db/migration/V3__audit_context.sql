alter table audit_events add column request_id varchar(80) not null default '-';
alter table audit_events add column source varchar(255) not null default 'SYSTEM';
alter table audit_events add column entity_type varchar(80) not null default 'SYSTEM';

create index idx_audit_events_request_id on audit_events (request_id);
create index idx_audit_events_entity_type on audit_events (entity_type);
