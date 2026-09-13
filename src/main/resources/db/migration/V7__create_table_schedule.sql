create table schedule
(
    id uuid primary key default gen_random_uuid(),
    id_professional uuid not null,
    date date not null,
    time time not null,
    created timestamptz default now(),

    constraint fk_professional foreign key (id_professional)
        references professionals(id)
);
