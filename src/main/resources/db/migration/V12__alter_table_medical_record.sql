ALTER TABLE medical_records

    ADD CONSTRAINT uc_medical_records UNIQUE (id_appointment);
