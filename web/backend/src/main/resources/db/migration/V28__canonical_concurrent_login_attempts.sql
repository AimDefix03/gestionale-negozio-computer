update login_attempts keeper
set attempts = (
        select sum(candidate.attempts)
        from login_attempts candidate
        where lower(trim(candidate.username_key)) = lower(trim(keeper.username_key))
    ),
    first_attempt_at = (
        select min(candidate.first_attempt_at)
        from login_attempts candidate
        where lower(trim(candidate.username_key)) = lower(trim(keeper.username_key))
    ),
    last_attempt_at = (
        select max(candidate.last_attempt_at)
        from login_attempts candidate
        where lower(trim(candidate.username_key)) = lower(trim(keeper.username_key))
    ),
    locked_until = (
        select max(candidate.locked_until)
        from login_attempts candidate
        where lower(trim(candidate.username_key)) = lower(trim(keeper.username_key))
    )
where keeper.id in (
    select min(candidate.id)
    from login_attempts candidate
    group by lower(trim(candidate.username_key))
);

delete from login_attempts row_to_delete
where row_to_delete.id <> (
    select min(keeper.id)
    from login_attempts keeper
    where lower(trim(keeper.username_key)) = lower(trim(row_to_delete.username_key))
);

update login_attempts
set username_key = lower(trim(username_key));

alter table login_attempts
    add constraint chk_login_attempts_username_key_canonical
        check (char_length(username_key) > 0 and username_key = lower(trim(username_key)));

alter table login_attempts
    add constraint chk_login_attempts_attempts_positive
        check (attempts > 0);

alter table login_attempts
    add constraint chk_login_attempts_timeline
        check (
            last_attempt_at >= first_attempt_at
            and (locked_until is null or locked_until >= last_attempt_at)
        );
