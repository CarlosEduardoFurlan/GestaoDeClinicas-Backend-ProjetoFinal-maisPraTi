# Checklist de validação das migrations

Antes de iniciar o desenvolvimento das entidades JPA, validar as migrations em um banco PostgreSQL vazio.

- [ ] Criar banco `clinica`.
- [ ] Executar a aplicação e o Flyway.
- [ ] Confirmar V1 até V13 sem falhas.
- [ ] Confirmar tabelas e foreign keys.
- [ ] Confirmar `users.password` com o tamanho definido pela migration vigente.
- [ ] Confirmar `UNIQUE(id_user)` em `professionals`.
- [ ] Confirmar `UNIQUE(id_appointment)` em `medical_records`.
- [ ] Confirmar `UNIQUE(id_professional, date, time)` em `schedule`.
- [ ] Depois de validar, não editar migrations já utilizadas sem alinhamento; novas alterações devem ser novas migrations.
