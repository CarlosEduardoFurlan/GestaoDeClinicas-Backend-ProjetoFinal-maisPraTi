ALTER TABLE medical_records
    ADD COLUMN notes text;

UPDATE medical_records
SET notes = concat_ws(E'\n\n',
        'Sintomas:' || E'\n' || symptoms,
        'Diagnóstico:' || E'\n' || diagnosis,
        'Prescrição:' || E'\n' || prescription);

ALTER TABLE medical_records
    ALTER COLUMN notes SET NOT NULL,
    DROP COLUMN symptoms,
    DROP COLUMN diagnosis,
    DROP COLUMN prescription;
