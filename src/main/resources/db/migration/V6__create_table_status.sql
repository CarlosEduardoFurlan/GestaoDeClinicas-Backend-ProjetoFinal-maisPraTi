create table status
(
    id uuid primary key default gen_random_uuid(),
    description varchar(35) not null unique
);