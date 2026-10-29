create table address
(
    id uuid primary key default gen_random_uuid(),
    address varchar(150) not null,
    number int not null,
    complement varchar(20),
    district varchar(50) not null,
    city varchar(100) not null,
    uf varchar(2) not null,
    zip_code varchar(9) not null,
    created timestamptz default now()
);
