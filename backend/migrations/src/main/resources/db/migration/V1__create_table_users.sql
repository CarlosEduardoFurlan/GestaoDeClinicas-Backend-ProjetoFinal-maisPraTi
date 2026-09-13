create table users (
      id uuid primary key default gen_random_uuid(),
      name varchar(100) not null,
      email varchar(100) not null unique,
      password varchar(20) not null,
      profile varchar(25) not null,
      created timestamptz default now()
);