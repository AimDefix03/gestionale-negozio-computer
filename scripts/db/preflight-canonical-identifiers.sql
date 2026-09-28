select lower(trim(username)) as canonical_value, count(*) as occurrences
from user_accounts
group by lower(trim(username))
having count(*) > 1;

select lower(trim(code)) as canonical_value, count(*) as occurrences
from products
group by lower(trim(code))
having count(*) > 1;

select lower(trim(code)) as canonical_value, count(*) as occurrences
from business_partners
group by lower(trim(code))
having count(*) > 1;
