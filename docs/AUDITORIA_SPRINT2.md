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
| #11 | arthurmorettipreuss06-sketch | originalmente cinco DTOs | packages com.clinica, @FutureOrPresent e limite de notes sem contrato; nenhum service; status/cancelamento/concorrência indefinidos | concluída após as decisões aprovadas; 183 testes PostgreSQL e CI verdes; mergeada em 7392b12 |

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

A branch feature/sprint2-api-preparation foi criada da develop após as PRs #12 e #14 e atualizada após o merge da PR #11. Ela prepara UserController, SpecialtyController e PUT/DELETE de ProfessionalController, sempre delegando aos services e usando DTOs e @Valid. A matriz administrativa aprovada foi aplicada no SecurityConfig e coberta com Security real.

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
| Professional PUT/DELETE | Controller preparado na feature | MVC e Security testados | Sim | exclusivo de ADMIN; FKs podem impedir delete |
| User CRUD administrativo | Controller preparado na feature | MVC e Security testados | Sim | exclusivo de ADMIN; delete respeita schema histórico |
| Specialty CRUD | Controller preparado na feature | MVC e Security testados | Sim | leitura pelos três perfis; escrita por ADMIN |
| Schedule create/list | Sim, develop após PR #11 | Sim, unitário e PostgreSQL | Sim | controller na integração final |
| Appointment service completo | Sim, develop após PR #11 | Sim, incluindo transições e concorrência | Sim | controller na integração final |
| Dupla reserva | Protegida no backend pelo lock do slot | Sim: duas transações simultâneas geram uma reserva e um conflito | Sim | sem UNIQUE em Appointment, conforme decisão |
| MedicalRecord entity/repository/UNIQUE | Modelo histórico presente | UNIQUE e opcionalidade testados na PR #11 | Sim | adequar para notes na integração final |
| MedicalRecord API | Não | Não | Decisão registrada | implementar modelo simplificado sem Measurement |
| Measurement | Não, conforme decisão | Não aplicável | Sim | aguardar schema formal |
| CORS externalizado | Sim na feature de preparação | Sim, config e HTTP | Sim | definir origem real do frontend por ambiente |
| Primeiro ADMIN | Provisionamento manual documentado | Procedimento não contém credencial | Sim | execução é responsabilidade do ambiente |
| Dashboard/relatórios | Não | Não aplicável | Sim | nenhum endpoint adicional aprovado |

## Integridade e bloqueios de modelagem

V13 cria UNIQUE em schedule(id_professional,date,time). Appointments armazena profissional/data/hora diretamente e não referencia Schedule. Em PostgreSQL 18 exclusivo, após V1–V13, uma transação inseriu um slot e dois appointments com o mesmo profissional/data/hora; ambas as inserções passaram e count(*) retornou 2. A transação foi revertida. Portanto, o banco não protege a dupla reserva de Appointment.

V14 cria o catálogo oficial de status. A reserva usa o slot único de V13 como recurso de sincronização: uma transação adquire lock pessimista no Schedule e então verifica Appointment ativo. Cancelados não bloqueiam o slot. Não foi criada UNIQUE em Appointment.

MedicalRecord representa Appointment 1:0..1 corretamente no schema/entity. A decisão de 22/10/2026 aprovou o modelo simplificado com notes e uma nova migration que converte os três campos históricos antes de removê-los. A implementação pertence à branch final de integração.

## Evidências de testes e gates

| Estado | Resultado |
| --- | --- |
| develop após PR #10 | 115 executados; zero falhas/erros/ignorados; PostgreSQL; Flyway V1–V13; JPA validate; JAR |
| PR #12 corrigida | 145 executados; zero falhas/erros/ignorados; PostgreSQL; verify; JAR; CI verde |
| develop após PR #12 | 145 executados; zero falhas/erros/ignorados; PostgreSQL; verify; JAR |
| develop após PR #14 | 145 executados; zero falhas/erros/ignorados; PostgreSQL; verify; JAR |
| PR #11 concluída | 183 executados; zero falhas/erros/ignorados; PostgreSQL; Flyway V1–V14; concorrência; verify; JAR; CI verde |
| feature de preparação, suíte convencional mais recente | 169 totais; 158 executados; 11 condicionados ao PostgreSQL ignorados; zero falhas/erros |
| feature de preparação, validação final PostgreSQL | 170 executados; zero falhas/erros/ignorados; Flyway V1–V13; JPA validate; verify; JAR |
| feature de preparação, runtime e smoke HTTP | JAR iniciado em localhost com banco exclusivo; 21 cenários aprovados |

O runtime usou banco PostgreSQL exclusivo e segredo JWT efêmero. Flyway criou o schema vazio, o JPA iniciou com ddl-auto=validate e o JAR respondeu aos smoke tests de login válido e inválido, token ausente e inválido, acesso permitido e negado, validação 400, recurso inexistente 404, rotas sem política 403, cadastro público negado, persistência de Patient, duplicidade, DELETE de Patient bloqueado e CORS configurado e não configurado. Credenciais, hash e token temporários foram removidos ao final.

## Estado de prontidão

O backend ainda não está concluído. As decisões anteriores foram aprovadas em 22/10/2026: status inicial Agendada, máquina de estados, cancelamento lógico, lock do Schedule, MedicalRecord simplificado e matriz administrativa. A PR #11 foi integrada. A próxima etapa é integrar a PR #15 e finalizar controllers de Schedule/Appointment, MedicalRecord, documentação e gates finais na branch `feature/sprint2-api-integration`.
