# Schedule duplicado — 409 uniforme

Data: 26/10/2026. Base develop: 9daa1c5567774ba9aa6726082f3747b01fab1343.
Branch: fix/schedule-duplicate-conflict.

## Causa e reprodução

ScheduleService verificava a existência do slot e lançava BusinessException,
tratada como 400. A mesma duplicidade detectada pela UNIQUE V13 produz
DataIntegrityViolationException, já tratada como 409. O status dependia do caminho.

Antes da correção, ScheduleDuplicatePostgresTest criou um slot por HTTP e
repetiu o mesmo profissional/data/hora. O teste que exige 409 falhou com
expected 409 / actual 400 na baseline, sem mudar serviço ou handler.

## Correção

Somente a exception do pre-check muda para a ConflictException existente.
Mensagem preservada: Horário já cadastrado para este profissional.
Mantidos pre-check, V13, migrations, FKs, DTOs, permissões e GlobalExceptionHandler.
docs/API.md agora explicita Schedule duplicado como 409 nos dois caminhos.

## Testes

ScheduleDuplicatePostgresTest não possui transação externa de teste. Usa
MockMvc/Spring Boot com login/JWT reais e PostgreSQL real. Cobre:

- Criação 201 e repetição sequencial 409, ConflictException e apenas um slot.
- Constraint real: depois do primeiro INSERT confirmado, somente o resultado
  do pre-check é forçado a falso para reproduzir deterministicamente a janela
  de corrida. INSERT, UNIQUE V13, rollback e tradução continuam reais;
  resolvida DataIntegrityViolationException e HTTP409, com apenas um registro.
- Dois requests concorrentes reais: exatamente 201/409 e apenas um slot.
- Profissional inexistente 404; DTO inválido 400.
- PROFESSIONAL próprio 201, alheio 403; RECEPTIONIST POST 403.

O teste sequencial não utiliza o resultado forçado. O spy é resetado entre
métodos pelo suporte de testes. A cobertura da constraint não substitui a prova
sequencial nem a concorrência real.

As expectativas antigas de BusinessException foram alinhadas somente nos testes
de Schedule (unitário e método específico do teste PostgreSQL combinado).
Nenhuma implementação ou contrato de MedicalRecord foi alterado.

Maven clean verify com RUN_POSTGRES_TESTS=true: **282 testes, zero falhas,
erros ou ignorados**, incluindo regressões de Schedule/Appointment.
Ambiente: Java26, PostgreSQL18 exclusivo em 127.0.0.1:55432, SCRAM, Flyway V1–V15
e JPA ddl-auto=validate. JAR Spring Boot gerado e smoke HTTP separado: criação
Schedule201, repetição409; Appointment em paralelo com um201/um409, cancelamento
lógico204 e registro preservado como Cancelada. Credenciais somente temporárias.

Comandos, com DB_URL/DB_USERNAME/DB_PASSWORD de banco descartável e Java26:

```text
mvnw.cmd -B -Dtest=ScheduleDuplicatePostgresTest test
mvnw.cmd -B clean verify
```

A revalidação específica no Chrome é executada após merge backend. O frontend
não precisa mudar código para adaptar 400 a 409; somente sua documentação de
baseline/histórico é atualizada. Não há ampliação para módulos posteriores.
