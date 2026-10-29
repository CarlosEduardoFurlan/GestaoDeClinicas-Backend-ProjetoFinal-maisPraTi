create table patients   
(
    id UUID primary key default gen_random_uuid(),
    name varchar(100) not null,
    cpf varchar(14) unique not null,
    date_birth date not null,
    telephone varchar(20),
    cell_phone varchar(20) not null,
    email varchar(100),
    created timestamp default CURRENT_TIMESTAMP,
    gender      varchar(1) not null,
    id_address uuid not null,

    constraint fk_address foreign key (id_address)
        references address(id) on delete cascade
);