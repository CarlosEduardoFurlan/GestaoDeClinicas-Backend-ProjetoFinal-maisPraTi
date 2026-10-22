# Agenda e agendamentos — conclusão técnica da PR 11

Revisão concluída em 22/10/2026 após aprovação das regras de negócio. A PR foi corrigida para usar os packages `br.com.clinica`, remover restrições não aprovadas e completar os services de Agenda e Appointment.

## Agenda

`ScheduleService` cria e lista slots. A criação exige profissional existente e rejeita duplicidade. O `UNIQUE` da V13 em `schedule(id_professional, date, time)` mantém somente um slot por profissional, data e hora.

## Appointment

O request de criação recebe paciente, profissional, data, hora e notes. O status não é recebido do cliente. A V14 insere o catálogo oficial: Agendada, Confirmada, Em atendimento, Concluída e Cancelada. O service localiza Agendada pela descrição única, sem UUID fixo no Java.

A criação é transacional. O service localiza o slot correspondente com lock pessimista de escrita, equivalente a `SELECT ... FOR UPDATE`, e somente depois verifica a ocupação. Appointment Cancelado não ocupa o horário. Uma reserva ativa no mesmo slot produz conflito 409.

As únicas transições aceitas são:

- Agendada → Confirmada ou Cancelada;
- Confirmada → Em atendimento ou Cancelada;
- Em atendimento → Concluída;
- Concluída e Cancelada são estados finais.

O cancelamento preserva o Appointment e seus vínculos, altera o status para Cancelada e libera o slot. O service não chama `repository.delete()`.

## Evidências

A suíte unitária cobre as cinco transições permitidas e todas as vinte combinações não permitidas. Um teste PostgreSQL dispara duas transações simultâneas contra o mesmo slot: uma reserva é criada e a outra recebe conflito. O mesmo teste cancela a reserva vencedora e comprova que o slot pode ser reservado novamente.

Em banco vazio, Flyway aplicou V1–V14, o JPA iniciou com `ddl-auto=validate` e 183 testes foram executados sem falhas, erros ou testes ignorados. O JAR foi gerado pelo `verify`.

Controllers, rotas e política HTTP de Agenda/Appointment pertencem à etapa de integração do Integrante 4.

## MedicalRecord

MedicalRecord não foi alterado nesta PR. Sua adequação ao modelo simplificado `id`, `appointment` e `notes` será feita em migration posterior na branch de integração, preservando dados históricos e a unicidade de `id_appointment`.
