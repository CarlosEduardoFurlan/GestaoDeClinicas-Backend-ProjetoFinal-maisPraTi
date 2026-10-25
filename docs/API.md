# API — +PraClinic

Estado em 25/10/2026. Contratos finais da Sprint 2 com os ajustes de compatibilidade necessários para o frontend.

## Autenticação e matriz de acesso

Enviar `Authorization: Bearer <accessToken>` nas rotas protegidas. Sem autenticação ou com token inválido/expirado: 401. Autenticado sem permissão: 403. Somente ADMIN, RECEPTIONIST e PROFESSIONAL são perfis reconhecidos.

| Método | Rota | Sucesso do controller | Acesso vigente |
| --- | --- | --- | --- |
| POST | /api/auth/login | 200 | Público |
| GET | /api/auth/me | 200 | ADMIN, RECEPTIONIST, PROFESSIONAL |
| GET | /api/statuses | 200 | ADMIN, RECEPTIONIST, PROFESSIONAL |
| GET | /api/patients | 200 | ADMIN, RECEPTIONIST |
| GET | /api/patients/{id} | 200 | ADMIN, RECEPTIONIST |
| POST | /api/patients | 201 | ADMIN, RECEPTIONIST |
| PUT | /api/patients/{id} | 200 | ADMIN, RECEPTIONIST |
| DELETE | /api/patients/{id} | 204 previsto, bloqueado | Nenhum perfil |
| GET | /api/professionals | 200 | ADMIN, RECEPTIONIST |
| GET | /api/professionals/{id} | 200 | ADMIN, RECEPTIONIST |
| POST | /api/professionals | 201 | ADMIN |
| PUT | /api/professionals/{id} | 200 | ADMIN |
| DELETE | /api/professionals/{id} | 204 | ADMIN |
| GET | /api/users | 200 | ADMIN |
| GET | /api/users/{id} | 200 | ADMIN |
| POST | /api/users | 201 | ADMIN |
| PUT | /api/users/{id} | 200 | ADMIN |
| DELETE | /api/users/{id} | 204 | ADMIN |
| GET | /api/specialties | 200 | ADMIN, RECEPTIONIST, PROFESSIONAL |
| GET | /api/specialties/{id} | 200 | ADMIN, RECEPTIONIST, PROFESSIONAL |
| POST | /api/specialties | 201 | ADMIN |
| PUT | /api/specialties/{id} | 200 | ADMIN |
| DELETE | /api/specialties/{id} | 204 | ADMIN |
| GET | /api/schedules | 200 | ADMIN e RECEPTIONIST: todos; PROFESSIONAL: próprios |
| POST | /api/schedules | 201 | ADMIN: qualquer Professional; PROFESSIONAL: próprio |
| GET | /api/appointments | 200 | ADMIN e RECEPTIONIST: todos; PROFESSIONAL: próprios |
| GET | /api/appointments/{id} | 200 | ADMIN e RECEPTIONIST; PROFESSIONAL: próprio |
| POST | /api/appointments | 201 | ADMIN, RECEPTIONIST |
| PUT | /api/appointments/{id} | 200 | ADMIN; RECEPTIONIST e PROFESSIONAL conforme transições abaixo |
| DELETE | /api/appointments/{id} | 204 | ADMIN, RECEPTIONIST |
| GET | /api/medical-records | 200 | ADMIN: todos; PROFESSIONAL: próprios |
| GET | /api/medical-records/{id} | 200 | ADMIN; PROFESSIONAL: próprio |
| POST | /api/medical-records | 201 | PROFESSIONAL, somente Appointment próprio |
| PUT | /api/medical-records/{id} | 200 | PROFESSIONAL, somente prontuário próprio |

PROFESSIONAL não recebe acesso global a profissionais, pacientes, agendas, agendamentos ou prontuários. O backend resolve o vínculo pelo subject do JWT (e-mail) até User e Professional; `professionalId` recebido do cliente nunca determina ownership. Usuário PROFESSIONAL sem Professional vinculado recebe 403.

Não existe cadastro público nem POST /api/auth/register. Toda rota não autorizada explicitamente permanece negada.

## Login

`POST /api/auth/login`, Content-Type application/json:

```json
{"email":"usuario@example.com","password":"senha-ficticia"}
```

Retorno 200: `{"accessToken":"JWT_ILUSTRATIVO","tokenType":"Bearer","expiresIn":900}`. expiresIn é duração em segundos. 400 para request inválido; 401 para credenciais incorretas. Perfil é obtido do banco, não enviado no login. JWT usa e-mail como subject; alteração desse identificador não integra o contrato de update.

`GET /api/auth/me` não recebe body e exige Bearer token de ADMIN, RECEPTIONIST ou PROFESSIONAL. O backend usa exclusivamente o subject/e-mail autenticado para localizar User e seu eventual Professional; nenhum identificador vem do cliente.

```json
{"userId":"00000000-0000-0000-0000-000000000001","name":"Ana Silva","email":"ana@example.com","profile":"PROFESSIONAL","professionalId":"00000000-0000-0000-0000-000000000002"}
```

Retorno 200 contém `userId,name,email,profile,professionalId`. `professionalId` é null para User sem Professional associado. A resposta não contém password/hash. Token ausente, inválido ou expirado retorna 401; User autenticado não localizado retorna 404.

## Pacientes

POST recebe `name` (obrigatório, até 100), `cpf` (obrigatório, até 14), `dateBirth` (data passada ISO YYYY-MM-DD), `telephone` (opcional, até 20), `cellPhone` (obrigatório, até 20), `email` (opcional, formato de e-mail, até 100), `gender` (obrigatório, um caractere) e `address` aninhado obrigatório.

```json
{"name":"Maria Silva","cpf":"12345678901","dateBirth":"1990-05-20","telephone":null,"cellPhone":"65999999999","email":"maria@example.com","gender":"F","address":{"address":"Rua A","number":100,"complement":null,"district":"Centro","city":"Cuiaba","uf":"MT","zipCode":"78000-000"}}
```

Address usa `address` (obrigatório, até 150), `number` (inteiro obrigatório), `complement` (opcional, até 20), `district` (obrigatório, até 50), `city` (obrigatório, até 100), `uf` (obrigatório, até 2) e `zipCode` (obrigatório, até 9). A criação do Address e do Patient ocorre na mesma transação; falha ao persistir Patient desfaz o Address.

PUT recebe os mesmos campos editáveis e o mesmo Address aninhado, exceto cpf. O backend atualiza o Address já associado e mantém seu identificador; CPF não é alterado. POST/PUT/GET por ID retornam PatientResponse com `id,name,cpf,dateBirth,telephone,cellPhone,email,gender,addressId,address`; GET listagem retorna uma lista desse objeto. Exemplo de response:

```json
{"id":"00000000-0000-0000-0000-000000000010","name":"Maria Silva","cpf":"12345678901","dateBirth":"1990-05-20","telephone":null,"cellPhone":"65999999999","email":"maria@example.com","gender":"F","addressId":"00000000-0000-0000-0000-000000000011","address":{"address":"Rua A","number":100,"complement":null,"district":"Centro","city":"Cuiaba","uf":"MT","zipCode":"78000-000"}}
```

POST retorna 201; GET/PUT retornam 200. Erros: 400 para validação e CPF/e-mail duplicado detectado pelo service; 404 para paciente inexistente ou vínculo de Address inválido; 409 para conflito de integridade do banco. As rotas exigem ADMIN ou RECEPTIONIST; token ausente/inválido retorna 401 e outro perfil retorna 403. DELETE continua bloqueado. Nenhuma cascade foi adicionada.

## Profissionais

POST recebe `name` (até 100), `register` (até 30), `telephone` e `cellPhone` (até 20), `email` de contato (até 100), `specialtyId` (UUID existente), `address` e `user`. Todos são obrigatórios.

Address: `address` (até 150), `number` (inteiro), `complement` (opcional, até 20), `district` (até 50), `city` (até 100), `uf` (até 2), `zipCode` (até 9); demais campos obrigatórios.

User: `name` (até 100), `email` (até 100), `password` (até 20), `profile`; todos obrigatórios. Perfil aceita exatamente ADMIN, RECEPTIONIST ou PROFESSIONAL, inclusive quando o service é chamado fora do HTTP. Senha armazenada com BCrypt.

```json
{"name":"Ana Silva","register":"CRM-12345","telephone":"6533333333","cellPhone":"65999999999","email":"contato@example.com","specialtyId":"00000000-0000-0000-0000-000000000000","address":{"address":"Rua A","number":100,"complement":null,"district":"Centro","city":"Cuiaba","uf":"MT","zipCode":"78000-000"},"user":{"name":"Ana Silva","email":"ana@example.com","password":"senha-ficticia","profile":"PROFESSIONAL"}}
```

A criação de usuário/endereço/profissional é transacional. ProfessionalResponseDTO contém `id,name,register,telephone,cellPhone,email,specialtyId,specialtyName,address,cityName,userId`. Address possui os mesmos sete campos de leitura e escrita descritos acima. `cityName` foi preservado por compatibilidade. Nenhuma senha ou hash é exposta. GET listagem retorna lista; GET por ID retorna objeto.

```json
{"id":"00000000-0000-0000-0000-000000000020","name":"Ana Silva","register":"CRM-12345","telephone":"6533333333","cellPhone":"65999999999","email":"contato@example.com","specialtyId":"00000000-0000-0000-0000-000000000021","specialtyName":"Clínica geral","address":{"address":"Rua A","number":100,"complement":null,"district":"Centro","city":"Cuiaba","uf":"MT","zipCode":"78000-000"},"cityName":"Cuiaba","userId":"00000000-0000-0000-0000-000000000022"}
```

PUT recebe os campos do cadastro profissional e address, sem user. Não muda e-mail de login, senha, perfil ou vínculo da conta. O e-mail de contato do profissional é um campo distinto do identificador de autenticação. DELETE exclui somente Professional, sem apagar User/Address/agenda/histórico; FKs impedem exclusão quando aplicável. PUT/DELETE são exclusivos de ADMIN.

Erros de contrato: 400 para validação/registro duplicado/e-mail de usuário duplicado; 404 para profissional/especialidade inexistente; 409 para conflito de integridade.

## Usuários administrativos

POST usa UserRequestDTO descrito acima. PUT usa `name,password,profile`, obrigatórios, com as mesmas validações. Não recebe e-mail. GET por ID/POST/PUT retornam `id,name,email,profile,created`; GET listagem retorna lista. created é fornecido pelo banco e pode ser nulo na resposta imediata de criação antes de recarregar o registro. Nenhuma resposta contém password.

Erros do contrato: 400 para request/perfil inválido/e-mail duplicado; 404 para usuário inexistente; 409 para conflito de integridade. Todas as operações são exclusivas de ADMIN. A migration histórica de professionals contém ON DELETE CASCADE a partir de users; nenhuma cascade foi criada ou ampliada nesta etapa.

## Especialidades

POST/PUT recebem `name` obrigatório até 50 caracteres e `description` opcional, sem limite artificial além do schema text. GET por ID/POST/PUT retornam `id,name,description,created`; GET listagem retorna lista. created pode ser nulo na resposta imediata de criação antes de recarga do banco.

400 para request/nome duplicado detectado pelo service; 404 para inexistente; 409 para violação de unicidade/FK. DELETE não exclui profissionais relacionados. Leitura é permitida aos três perfis; escrita é exclusiva de ADMIN. Não existe estado ativo/inativo de profissional.

## Catálogo de status

`GET /api/statuses` não recebe body e exige Bearer token de ADMIN, RECEPTIONIST ou PROFESSIONAL. Retorna 200 com DTOs `id,description` dos cinco status oficiais persistidos pela V14:

```json
[
  {"id":"00000000-0000-0000-0000-000000000031","description":"Agendada"},
  {"id":"00000000-0000-0000-0000-000000000032","description":"Confirmada"},
  {"id":"00000000-0000-0000-0000-000000000033","description":"Em atendimento"},
  {"id":"00000000-0000-0000-0000-000000000034","description":"Concluída"},
  {"id":"00000000-0000-0000-0000-000000000035","description":"Cancelada"}
]
```

Token ausente, inválido ou expirado retorna 401. Não existem POST, PUT, PATCH ou DELETE para Status; essas tentativas permanecem negadas com 403 para usuário autenticado.

## Agenda, agendamentos e prontuários

### Agenda

`POST /api/schedules` recebe `professionalId`, `date` e `time`, todos obrigatórios. O profissional deve existir e V13 garante somente um slot por profissional/data/hora. `GET /api/schedules` retorna `id,professionalId,professionalName,date,time,created`.

ADMIN cria slots para qualquer Professional e lista todos. RECEPTIONIST somente lista todos. PROFESSIONAL lista apenas os próprios slots e só cria quando o `professionalId` corresponde ao Professional resolvido pelo usuário autenticado.

### Agendamentos

`POST /api/appointments` recebe `patientId`, `professionalId`, `date`, `time` e `notes` opcional. Não recebe status. O backend atribui Agendada, buscando a descrição única criada pela V14.

A criação exige paciente, profissional e slot existentes. A transação bloqueia o slot com lock pessimista antes de verificar Appointment ativo. Reserva ativa no mesmo horário retorna 409. Appointment Cancelado não bloqueia o slot.

`PUT /api/appointments/{id}` recebe `statusId`. Transições aceitas: Agendada para Confirmada ou Cancelada; Confirmada para Em atendimento ou Cancelada; Em atendimento para Concluída. Concluída e Cancelada são finais. Transição inválida retorna 400.

ADMIN pode executar qualquer transição válida. RECEPTIONIST pode executar somente Agendada para Confirmada; cancelamento usa DELETE. PROFESSIONAL, somente no próprio Appointment, pode executar Confirmada para Em atendimento e Em atendimento para Concluída. Uma transição válida para a máquina, mas vedada ao ator, retorna 403.

`DELETE /api/appointments/{id}` preserva o registro, altera o status para Cancelada e retorna 204. Appointment Concluída ou já Cancelada não pode ser cancelada. GET lista ou retorna `id,patientId,patientName,professionalId,professionalName,date,time,statusId,statusName,notes,created`.

ADMIN e RECEPTIONIST criam e cancelam Appointments. PROFESSIONAL não cria nem cancela. Nas leituras, ADMIN e RECEPTIONIST recebem todos; PROFESSIONAL recebe somente os vinculados ao próprio Professional e obtém 403 ao consultar identificador alheio.

### Prontuários

MedicalRecord possui somente `id`, `appointment` e `notes`. V15 converte registros históricos para texto legível antes de remover symptoms, diagnosis e prescription, mantém `notes` obrigatório e preserva `UNIQUE(id_appointment)`.

`POST /api/medical-records` recebe `appointmentId` e `notes`, ambos obrigatórios. Somente um prontuário pode existir por Appointment; duplicidade retorna 409. GET lista/consulta `id,appointmentId,notes`. PUT altera somente notes. Não existe DELETE de prontuário.

ADMIN pode somente ler todos os prontuários. RECEPTIONIST não possui acesso. PROFESSIONAL lê, cria e altera somente prontuários ligados aos próprios Appointments. ADMIN e RECEPTIONIST não podem criar ou alterar conteúdo clínico.

Measurement não tem schema aprovado e não foi implementado. Não existem endpoints adicionais de dashboard/relatórios.

## Erros

Formato único ApiErrorResponse: `timestamp,status,error,message,path,details`. details contém erros de validação quando aplicável.

```json
{"timestamp":"2026-10-20T21:00:00","status":404,"error":"Not Found","message":"Paciente não encontrado","path":"/api/patients/00000000-0000-0000-0000-000000000000","details":null}
```

| Código | Situação |
| --- | --- |
| 400 | Validação, JSON/UUID inválido, BusinessException |
| 401 | Não autenticado: token ausente, inválido ou expirado; login incorreto |
| 403 | Autenticado sem permissão de perfil ou sem ownership do recurso |
| 404 | Recurso inexistente, quando a rota está autorizada |
| 405 | Método não suportado, quando alcança o MVC |
| 409 | Unicidade/FK do banco, Appointment ativo no slot ou segundo prontuário |
| 500 | Erro inesperado, sem detalhes internos expostos |

## CORS

`APP_CORS_ALLOWED_ORIGINS` configura `app.cors.allowed-origins`: lista de origens exatas separadas por vírgula, por exemplo `http://localhost:5173` quando essa for a origem acordada do ambiente. Vazio por padrão: nenhuma origem externa liberada. Não usar wildcard, caminho, query, fragmento ou credenciais na origem. Métodos GET/POST/PUT/DELETE/OPTIONS; headers Authorization e Content-Type; sem cookies/credentials. Preflight aceito não concede autenticação nem libera rotas bloqueadas.

## Testes

`./mvnw.cmd test` no Windows ou `./mvnw test` no Linux/macOS. Sem RUN_POSTGRES_TESTS=true, os testes condicionados a PostgreSQL são ignorados intencionalmente. Para executar todos, usar banco exclusivo, configurar DB_URL/DB_USERNAME/DB_PASSWORD e RUN_POSTGRES_TESTS=true, executar verify. Não usar banco de desenvolvimento.

SecurityHttpTest usa filtros/JWT reais e services simulados, incluindo `/api/auth/me`, `/api/statuses` e ausência de endpoints mutáveis de Status. OwnershipPolicyServiceTest cobre as permissões e transições por ator. CurrentUserAccessTest cobre a resolução pelo subject e o vínculo ausente. OwnershipPostgresTest prova filtros e 403 entre dois profissionais persistidos. ApiPostgresTest cobre login, BCrypt, catálogo oficial, identidade autenticada, contratos de Professional e fluxo transacional de Patient/Address com persistência real. Os números e gates desta rodada estão no relatório de auditoria; contagem não representa cobertura percentual.
