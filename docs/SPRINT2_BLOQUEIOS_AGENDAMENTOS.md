# Agenda e agendamentos — revisão da PR 11

Revisão em 20/10/2026. A PR original acrescentava somente cinco DTOs; não entregava ScheduleService nem AppointmentService. Não alterava entities, migrations ou security. Não havia testes próprios nem execução de CI no head original.

## Trabalho independente das decisões pendentes

Packages corrigidos para br.com.clinica.dto.request/response. Removidos @FutureOrPresent e limite de 500 caracteres de notes, sem base no contrato aprovado. ScheduleResponseDTO.created usa OffsetDateTime, conforme Schedule e timestamptz.

ScheduleService implementa criação/listagem: verifica profissional existente e duplicidade; o UNIQUE V13 protege gravações concorrentes de slots. AppointmentService fornece apenas listagem com IDs dos vínculos reais. Nenhuma permissão nova é liberada.

AppointmentRequestDTO e AppointmentStatusUpdateRequestDTO continuam rascunhos de contrato sem consumidor de escrita. statusId obrigatório é proposta original da PR, NÃO uma decisão homologada. Não criar controller de escrita usando esses DTOs antes do alinhamento.

## Bloqueio de criação e reserva concorrente

O guia atualizado do integrante 3 exige decisão sobre receber statusId na criação ou escolher status no servidor. Não há catálogo inicial aprovado nem matriz de transições/cancelamento nos documentos disponíveis.

A V13 protege schedule(id_professional,date,time). A V8 armazena esses dados diretamente em appointments, sem UNIQUE equivalente nem FK para um slot. Teste SQL transacional executado no PostgreSQL 18 exclusivo, após Flyway V1–V13, inseriu um slot e dois appointments com o mesmo profissional/data/hora: ambas as inserções foram aceitas; count(*) = 2. ROLLBACK removeu todos os dados da demonstração.

Uma nova migration poderia acrescentar UNIQUE(id_professional,date,time) em appointments, mas isso também impede reutilizar o horário se o cancelamento preservar a linha. Outra solução exigiria definir quais status ocupam o horário e seu efeito sobre a reserva. Não há autorização documental para escolher essa regra; o guia exige aprovação antes da constraint. Nenhuma migration foi criada ou alterada.

Decisões necessárias: contrato de status inicial, transições permitidas, significado de cancelamento e reutilização de slot, e mecanismo de integridade compatível. POST/PUT/DELETE de appointments permanecem sem implementação até resolver esses pontos. A PR não deve ser mergeada como entrega completa enquanto permanecerem pendentes.

## MedicalRecord

Entity/repository existem e refletem V9/V12. Appointment pode existir sem registro clínico; UNIQUE(id_appointment) impede dois registros, verificado com PostgreSQL. Não existem DTO/service/controller clínicos.

A tela atual MedicalRecordsPage.tsx do frontend develop usa um campo notes e consulta relacionada. O schema exige symptoms, diagnosis e prescription não nulos. Essa divergência precisa de decisão formal; não foi feita conversão implícita nem mudança de schema.

Referência de leitura, sem alterações no frontend: https://github.com/CarlosEduardoFurlan/GestaoDeClinicas-Frontend-ProjetoFinal-maisPraTi/blob/develop/clinica-frontend/src/pages/MedicalRecords/MedicalRecordsPage.tsx
