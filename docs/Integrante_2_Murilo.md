# Revisão de pacientes — Sprint 2 — Integrante 2

Responsável pela entrega original: Murilo. Revisão de integração em 20/10/2026.

## Conteúdo efetivamente entregue

A PR #13 acrescentava apenas este relatório contra main. O CRUD de pacientes, DTOs, repository e os 11 testes de PatientService já estavam na develop; não há código funcional novo nessa entrega.

A branch original partiu do histórico de main. Sua comparação de merge com develop incluía também 821 linhas de alteração no README. A integração transportou somente o commit documental 6231d32 para uma branch limpa da develop, preservando a autoria. Nenhuma exclusão de código nem substituição do README foi transportada.

## Contratos preservados

PatientService oferece create, findAll, findById, update e delete. Criação e atualização validam endereço existente e e-mail duplicado; criação valida CPF duplicado. Atualização não altera CPF. Recursos inexistentes usam ResourceNotFoundException (404); duplicidades detectadas pelo service usam BusinessException (400).

POST, GET listagem, GET por ID e PUT de pacientes exigem ADMIN ou RECEPTIONIST. PROFESSIONAL não tem acesso global aos pacientes.

## Exclusão e histórico

DELETE /api/patients/{id} permanece bloqueado pela segurança nesta Sprint. O método do service ainda faz exclusão física; appointments possui FK para patients, e medical_records depende de appointments. Não foi implementada nova política de exclusão, cascade, soft delete, anonimização ou alteração de schema.

O bloqueio HTTP é a decisão vigente. Uma política futura de exclusão/preservação exige aprovação explícita antes de mudar service ou liberar a rota.

## Verificação de integração

Após integrar a entrega corrigida do integrante 1, Maven Wrapper/JDK 26 com PostgreSQL 18 exclusivo executou 145 testes: zero falhas, erros ou ignorados. Os 11 casos de PatientServiceTest fazem parte desse total. Este relatório não atribui os testes ou o CRUD preexistentes a código novo da PR #13.
