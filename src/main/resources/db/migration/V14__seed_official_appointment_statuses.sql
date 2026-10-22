INSERT INTO status (description)
VALUES ('Agendada'),
       ('Confirmada'),
       ('Em atendimento'),
       ('Concluída'),
       ('Cancelada')
ON CONFLICT (description) DO NOTHING;
