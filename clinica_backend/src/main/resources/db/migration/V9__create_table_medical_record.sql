create table medical_records
(
    id uuid primary key default gen_random_uuid(),
    id_appointment uuid not null,
    symptoms text not null ,
    diagnosis text not null,
    prescription text not null ,

    constraint fk_appointment foreign key (id_appointment)
        references appointments(id)
        on delete restrict
);