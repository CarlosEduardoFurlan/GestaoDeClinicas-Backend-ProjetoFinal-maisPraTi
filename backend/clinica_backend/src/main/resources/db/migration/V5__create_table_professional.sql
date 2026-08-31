create table professionals
(
    id       uuid primary key default gen_random_uuid(),
    name     varchar(100) not null,
    register      varchar(30)  not null unique,
    telephone varchar(20)  not null,
    cell_phone  varchar(20)  not null,
    email    varchar(100) not null,
    id_speciality uuid not null,
    id_address uuid not null,
    id_user uuid not null,

    constraint fk_speciality foreign key(id_speciality)
        references specialties(id)
        on delete restrict,

    constraint fk_address foreign key (id_address)
        references address(id)
        on delete cascade,

    constraint fk_user foreign key (id_user)
        references users(id)
        on delete cascade
);
