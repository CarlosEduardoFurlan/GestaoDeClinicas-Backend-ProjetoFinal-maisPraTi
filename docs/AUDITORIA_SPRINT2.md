# Auditoria técnica da Sprint 2 do backend

Estado em 21/10/2026. Este documento registra fatos verificados na develop e nas PRs da Sprint 2 do plano atual de quatro integrantes. Não declara o backend concluído.

## Estado inicial

A develop local estava em 1b59071 e foi atualizada para cb59842, merge da PR #10. Dois arquivos locais preexistentes e não versionados, CHECKLIST_INTEGRANTE_4.md e CONTEXTO_CONTINUIDADE.md, foram preservados e não entraram em commits. A base após PR #10 executou 115 testes com PostgreSQL exclusivo, zero falhas, erros ou ignorados, aplicou Flyway V1–V13, validou JPA e gerou o JAR.

As PRs #11, #12 e #13 apontavam para main. Todas foram retargetadas para develop antes de qualquer integração.

## PRs analisadas e integração

| PR | Autor | Delta real contra develop | Problemas principais | Resultado |
| --- | --- | --- | --- | --- |
| #12 | augustomakar | UserService, SpecialtyService, complemento de ProfessionalService, DTOs e testes | testes tratavam USER como válido; update mudava e-mail usado pelo JWT; ProfessionalService não validava profile; delete apagava User; contrato de update exigia dados de autenticação | corrigida no commit 1029147; CI verde; mergeada primeiro em dc4bceb |
| #13 | MuriloMfu | um relatório; ao retargetar, README antigo também aparecia por histórico divergente | descrição atribuía CRUD/testes preexistentes a código novo; branch não estava limpa sobre develop | substituída pela PR #14, preservando o commit/autoria e somente o relatório revisado; CI verde; mergeada depois em 7cd90d0; #13 fechada sem merge |
| #11 | arthurmorettipreuss06-sketch | originalmente cinco DTOs | packages com.clinica, @FutureOrPresent e limite de notes sem contrato; nenhum service; status/cancelamento/concorrência indefinidos | corrigida parcialmente no commit c8c3f22; CI verde; continua aberta como WIP e não foi mergeada |

Após cada merge realizado, develop foi atualizada e validada com PostgreSQL exclusivo: 145 testes, zero falhas, erros ou ignorados, build e JAR aprovados.

## Correções incorporadas na develop

- Perfis aceitos exatamente: ADMIN, RECEPTIONIST e PROFESSIONAL, também nos services.
- Novas senhas passam por BCrypt e responses não expõem password/hash.
- UserUpdateRequestDTO não altera e-mail enquanto a identidade JWT depende desse campo e não há política aprovada.
- ProfessionalUpdateRequestDTO preserva a conta de autenticação associada.
- Exclusão de Professional não apaga User, Address, Schedule, Appointment ou histórico.
- Exclusão de Specialty respeita as FKs; não existe conceito ativo/inativo.
- Relatório do integrante 2 registra que CRUD/DTOs/testes de Patient já existiam e que DELETE HTTP permanece bloqueado.
- Nenhuma migration histórica foi alterada.

## Trabalho na feature de preparação da API

A branch feature/sprint2-api-preparation foi criada da develop após as PRs #12 e #14. Ela prepara UserController, SpecialtyController e PUT/DELETE de ProfessionalController, sempre delegando aos services e usando DTOs e @Valid. Essas rotas permanecem negadas pelo SecurityConfig porque sua matriz de acesso não foi aprovada. Testes MVC isolados comprovam os códigos pretendidos; testes com Security real comprovam 401/403 e que os services não são chamados.

CORS foi externalizado por APP_CORS_ALLOWED_ORIGINS, vazio por padrão, sem wildcard, caminhos, query, fragmentos ou userinfo. Não altera autenticação/autorização. O bootstrap manual e seguro do primeiro ADMIN foi documentado sem credencial ou hash real.

O fluxo HTTP de Patient foi ampliado em ApiPostgresTest para criação, consulta, atualização, 404, validação e bloqueio de DELETE com persistência real para ADMIN e RECEPTIONIST.

## Matriz de requisitos

| Requisito | Implementado? | Testado? | Documentado? | Pendência |
| --- | --- | --- | --- | --- |
| Login/BCrypt/JWT/400/401/403 | Sim, develop | Sim, unitário, HTTP e PostgreSQL | Sim, API.md | nenhuma no escopo aprovado |
| Perfis ADMIN/RECEPTIONIST/PROFESSIONAL | Sim | Sim, HTTP e service | Sim | não criar PATIENT |
| Cadastro público | Ausente, conforme decisão | Sim, rota negada | Sim | nenhuma |
| Patient POST/GET/GET id/PUT | Sim | Sim, service, HTTP e PostgreSQL | Sim | frontend precisa de origem aprovada para addressId |
| Patient DELETE | Service físico existe; HTTP bloqueado | Bloqueio testado | Sim | política de preservação do histórico não aprovada |
| Professional POST/GET/GET id | Sim | Sim | Sim | nenhuma no contrato vigente |
| Professional PUT/DELETE | Controller preparado na feature | MVC e bloqueio Security testados | Sim | matriz de acesso não aprovada; FKs podem impedir delete |
| User CRUD administrativo | Controller preparado na feature | MVC e bloqueio Security testados | Sim | matriz de acesso não aprovada; delete exige revisar ON DELETE CASCADE histórico |
| Specialty CRUD | Controller preparado na feature | MVC e bloqueio Security testados | Sim | matriz de acesso não aprovada |
| Schedule create/list | Implementado na PR #11 WIP | Sim, unitário e PostgreSQL na PR | Sim | PR não integrada; permissões não aprovadas |
| Appointment list | Implementado na PR #11 WIP | Sim | Sim | PR não integrada; permissões não aprovadas |
| Appointment create/update/delete | Não | Lacuna demonstrada | Sim | status inicial, transições, cancelamento e concorrência |
| Dupla reserva no banco | Não para appointments | Sim: banco aceitou duas linhas iguais | Sim | decisão necessária antes de nova migration |
| MedicalRecord entity/repository/UNIQUE | Sim | UNIQUE e opcionalidade testados na PR #11 | Sim | DTO/service/controller bloqueados pela divergência de modelo |
| MedicalRecord API | Não | Não | Bloqueio documentado | frontend usa notes; schema exige symptoms/diagnosis/prescription |
| Measurement | Não, conforme decisão | Não aplicável | Sim | aguardar schema formal |
| CORS externalizado | Sim na feature de preparação | Sim, config e HTTP | Sim | definir origem real do frontend por ambiente |
| Primeiro ADMIN | Provisionamento manual documentado | Procedimento não contém credencial | Sim | execução é responsabilidade do ambiente |
| Dashboard/relatórios | Não | Não aplicável | Sim | nenhum endpoint adicional aprovado |

## Integridade e bloqueios de modelagem

V13 cria UNIQUE em schedule(id_professional,date,time). Appointments armazena profissional/data/hora diretamente e não referencia Schedule. Em PostgreSQL 18 exclusivo, após V1–V13, uma transação inseriu um slot e dois appointments com o mesmo profissional/data/hora; ambas as inserções passaram e count(*) retornou 2. A transação foi revertida. Portanto, o banco não protege a dupla reserva de Appointment.

Uma UNIQUE simples em appointments impediria também reutilização de horário após cancelamento se a linha for preservada. Como status inicial, transições, cancelamento e efeito sobre disponibilidade não foram aprovados, nenhuma V14 foi criada.

MedicalRecord representa Appointment 1:0..1 corretamente no schema/entity. A UI atual usa um único campo notes; V9 exige symptoms, diagnosis e prescription não nulos. Nenhum documento posterior fornecido resolve a divergência. Nenhum endpoint ou migration clínica foi inventado.

## Evidências de testes e gates

| Estado | Resultado |
| --- | --- |
| develop após PR #10 | 115 executados; zero falhas/erros/ignorados; PostgreSQL; Flyway V1–V13; JPA validate; JAR |
| PR #12 corrigida | 145 executados; zero falhas/erros/ignorados; PostgreSQL; verify; JAR; CI verde |
| develop após PR #12 | 145 executados; zero falhas/erros/ignorados; PostgreSQL; verify; JAR |
| develop após PR #14 | 145 executados; zero falhas/erros/ignorados; PostgreSQL; verify; JAR |
| PR #11 WIP | 154 executados; zero falhas/erros/ignorados; PostgreSQL; verify; JAR; CI verde |
| feature de preparação, suíte convencional mais recente | 169 totais; 158 executados; 11 condicionados ao PostgreSQL ignorados; zero falhas/erros |
| feature de preparação, última execução PostgreSQL anterior à retomada | 170 executados; zero falhas/erros/ignorados; verify; JAR |

O PostgreSQL temporário foi encerrado entre as sessões. A repetição final de PostgreSQL/runtime da feature de preparação ficou impedida por limite do revisor automático de escalonamento; a tentativa não foi executada. O código posterior acrescentou apenas documentação e o mapeamento externalizado de CORS em application.properties/.env.example, cobertos pela suíte convencional para configuração/HTTP.

## Estado de prontidão

O backend não está concluído e ainda não está pronto para integração total com o frontend. Patient e os contratos já liberados de Professional/Auth estão utilizáveis. Controllers administrativos estão preparados, porém bloqueados por política. Agenda/agendamentos e prontuário dependem das decisões objetivas registradas abaixo. A branch final feature/sprint2-api-integration não foi criada porque o gate solicitado exige primeiro incorporar as entregas válidas dos integrantes 1, 2 e 3; a PR #11 do integrante 3 ainda não atende esse gate.

## Decisões necessárias

1. Appointment: POST recebe statusId existente ou o backend escolhe um status inicial? Se escolhe, qual registro/identificador define esse status sem seed aprovado?
2. Appointment: quais transições são permitidas, o que DELETE significa e quando um cancelamento libera o horário?
3. Integridade: com a regra anterior definida, aprovar V14 para proteger dupla reserva ou aprovar outro modelo compatível com cancelamento/histórico.
4. MedicalRecord: manter symptoms/diagnosis/prescription do schema ou aprovar nova migration para o modelo simplificado de notes do protótipo?
5. Segurança administrativa: quais perfis podem usar User, Specialty e PUT/DELETE de Professional? Até a decisão, todos permanecem bloqueados.
