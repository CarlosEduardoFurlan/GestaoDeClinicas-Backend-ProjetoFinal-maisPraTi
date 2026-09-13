ALTER TABLE schedule
    ADD CONSTRAINT uc_schedule UNIQUE (id_professional, date, time);
