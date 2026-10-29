create table specialties
(
    id uuid primary key default gen_random_uuid(),
    name varchar(50) not null unique,
    description text,
    created timestamptz default now()
);
