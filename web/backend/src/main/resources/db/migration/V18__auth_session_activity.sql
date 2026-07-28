alter table auth_sessions add column last_used_at timestamp(6);

update auth_sessions
set last_used_at = created_at
where last_used_at is null;

alter table auth_sessions alter column last_used_at set not null;

create index idx_auth_sessions_last_used_at on auth_sessions (last_used_at);
