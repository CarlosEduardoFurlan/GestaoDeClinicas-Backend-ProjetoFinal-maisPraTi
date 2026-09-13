
create table appointments
(
    id uuid primary key default gen_random_uuid(),
    id_patient uuid not null,
    id_professional uuid not null,
    date date not null,
    time time not null,
    id_status uuid not null,
    created timestamp not null default current_timestamp,
    notes text,

    constraint fk_patient foreign key(id_patient)
        references patients(id),

    constraint fk_professional foreign key (id_professional)
        references professionals(id),

    constraint fk_status foreign key(id_status)
        references status(id)
);