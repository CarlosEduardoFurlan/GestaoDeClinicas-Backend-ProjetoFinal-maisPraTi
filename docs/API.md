# API — +PraClinic

Estado em 22/10/2026. Contratos desta feature de preparação, atualizada após a integração da PR #11. A etapa seguinte finaliza controllers de agenda, agendamentos e prontuários.

## Autenticação e matriz de acesso

Enviar `Authorization: Bearer <accessToken>` nas rotas protegidas. Sem autenticação ou com token inválido/expirado: 401. Autenticado sem permissão: 403. Somente ADMIN, RECEPTIONIST e PROFESSIONAL são perfis reconhecidos.

| Método | Rota | Sucesso do controller | Acesso vigente |
| --- | --- | --- | --- |
| POST | /api/auth/login | 200 | Público |
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
| GET | /api/schedules | 200 previsto, bloqueado | Nenhum perfil até decisão |
| POST | /api/schedules | 201 previsto, bloqueado | Nenhum perfil até decisão |
| GET | /api/appointments | 200 previsto, bloqueado | Nenhum perfil até decisão |
| GET | /api/appointments/{id} | 200 previsto, bloqueado | Nenhum perfil até decisão |
| POST | /api/appointments | 201 previsto, bloqueado | Nenhum perfil até decisão |
| PUT | /api/appointments/{id} | 200 previsto, bloqueado | Nenhum perfil até decisão |
| DELETE | /api/appointments/{id} | 204 previsto, bloqueado | Nenhum perfil até decisão |
| GET | /api/medical-records | 200 previsto, bloqueado | Nenhum perfil até decisão |
| GET | /api/medical-records/{id} | 200 previsto, bloqueado | Nenhum perfil até decisão |
| POST | /api/medical-records | 201 previsto, bloqueado | Nenhum perfil até decisão |
| PUT | /api/medical-records/{id} | 200 previsto, bloqueado | Nenhum perfil até decisão |

PROFESSIONAL não recebe acesso global a profissionais, pacientes ou agendamentos. A leitura de especialidades é a única operação administrativa global liberada para esse perfil nesta etapa.

Não existe cadastro público nem POST /api/auth/register. Toda rota não autorizada explicitamente permanece negada. Os controllers de Agenda, Appointment e MedicalRecord estão implementados, mas o filtro retorna 403 para qualquer perfil até aprovação de sua matriz de acesso.

## Login

`POST /api/auth/login`, Content-Type application/json:

```json
{"email":"usuario@example.com","password":"senha-ficticia"}
```

Retorno 200: `{"accessToken":"JWT_ILUSTRATIVO","tokenType":"Bearer","expiresIn":900}`. expiresIn é duração em segundos. 400 para request inválido; 401 para credenciais incorretas. Perfil é obtido do banco, não enviado no login. JWT usa e-mail como subject; alteração desse identificador não integra o contrato de update.

## Pacientes

POST recebe `name` (obrigatório, até 100), `cpf` (obrigatório, até 14), `dateBirth` (data passada ISO YYYY-MM-DD), `telephone` (opcional, até 20), `cellPhone` (obrigatório, até 20), `email` (opcional, formato de e-mail, até 100), `gender` (obrigatório, um caractere) e `addressId` (UUID de endereço existente).

```json
{"name":"Maria Silva","cpf":"12345678901","dateBirth":"1990-05-20","telephone":null,"cellPhone":"65999999999","email":"maria@example.com","gender":"F","addressId":"00000000-0000-0000-0000-000000000000"}
```

UUID ilustrativo: substituir por endereço existente. Não há endpoint de endereços aprovado nesta etapa; a origem desse addressId no fluxo do frontend precisa ser alinhada. Não foi inventado cadastro aninhado para Patient.

PUT recebe os mesmos campos, exceto cpf. CPF não é alterado. POST/PUT/GET por ID retornam PatientResponse com `id,name,cpf,dateBirth,telephone,cellPhone,email,gender,addressId`; GET listagem retorna uma lista desse objeto.

Erros: 400 para validação e CPF/e-mail duplicado detectado pelo service; 404 para paciente/endereço inexistente; 409 para conflito de integridade do banco. DELETE continua bloqueado: service faz exclusão física e a FK de appointments preserva os vínculos existentes. Nenhuma política de soft delete, anonimização ou cascade foi adicionada.

## Profissionais

POST recebe `name` (até 100), `register` (até 30), `telephone` e `cellPhone` (até 20), `email` de contato (até 100), `specialtyId` (UUID existente), `address` e `user`. Todos são obrigatórios.

Address: `address` (até 150), `number` (inteiro), `complement` (opcional, até 20), `district` (até 50), `city` (até 100), `uf` (até 2), `zipCode` (até 9); demais campos obrigatórios.

User: `name` (até 100), `email` (até 100), `password` (até 20), `profile`; todos obrigatórios. Perfil aceita exatamente ADMIN, RECEPTIONIST ou PROFESSIONAL, inclusive quando o service é chamado fora do HTTP. Senha armazenada com BCrypt.

```json
{"name":"Ana Silva","register":"CRM-12345","telephone":"6533333333","cellPhone":"65999999999","email":"contato@example.com","specialtyId":"00000000-0000-0000-0000-000000000000","address":{"address":"Rua A","number":100,"complement":null,"district":"Centro","city":"Cuiaba","uf":"MT","zipCode":"78000-000"},"user":{"name":"Ana Silva","email":"ana@example.com","password":"senha-ficticia","profile":"PROFESSIONAL"}}
```

A criação de usuário/endereço/profissional é transacional. Retorno ProfessionalResponseDTO: `id,name,register,telephone,cellPhone,email,specialtyName,cityName,userId`. Nenhuma senha ou hash é exposta. GET listagem retorna lista; GET por ID retorna objeto.

PUT recebe os campos do cadastro profissional e address, sem user. Não muda e-mail de login, senha, perfil ou vínculo da conta. O e-mail de contato do profissional é um campo distinto do identificador de autenticação. DELETE exclui somente Professional, sem apagar User/Address/agenda/histórico; FKs impedem exclusão quando aplicável. PUT/DELETE são exclusivos de ADMIN.

Erros de contrato: 400 para validação/registro duplicado/e-mail de usuário duplicado; 404 para profissional/especialidade inexistente; 409 para conflito de integridade.

## Usuários administrativos

POST usa UserRequestDTO descrito acima. PUT usa `name,password,profile`, obrigatórios, com as mesmas validações. Não recebe e-mail. GET por ID/POST/PUT retornam `id,name,email,profile,created`; GET listagem retorna lista. created é fornecido pelo banco e pode ser nulo na resposta imediata de criação antes de recarregar o registro. Nenhuma resposta contém password.

Erros do contrato: 400 para request/perfil inválido/e-mail duplicado; 404 para usuário inexistente; 409 para conflito de integridade. Todas as operações são exclusivas de ADMIN. A migration histórica de professionals contém ON DELETE CASCADE a partir de users; nenhuma cascade foi criada ou ampliada nesta etapa.

## Especialidades

POST/PUT recebem `name` obrigatório até 50 caracteres e `description` opcional, sem limite artificial além do schema text. GET por ID/POST/PUT retornam `id,name,description,created`; GET listagem retorna lista. created pode ser nulo na resposta imediata de criação antes de recarga do banco.

400 para request/nome duplicado detectado pelo service; 404 para inexistente; 409 para violação de unicidade/FK. DELETE não exclui profissionais relacionados. Leitura é permitida aos três perfis; escrita é exclusiva de ADMIN. Não existe estado ativo/inativo de profissional.

## Agenda, agendamentos e prontuários

### Agenda

`POST /api/schedules` recebe `professionalId`, `date` e `time`, todos obrigatórios. O profissional deve existir e V13 garante somente um slot por profissional/data/hora. `GET /api/schedules` retorna `id,professionalId,professionalName,date,time,created`.

### Agendamentos

`POST /api/appointments` recebe `patientId`, `professionalId`, `date`, `time` e `notes` opcional. Não recebe status. O backend atribui Agendada, buscando a descrição única criada pela V14.

A criação exige paciente, profissional e slot existentes. A transação bloqueia o slot com lock pessimista antes de verificar Appointment ativo. Reserva ativa no mesmo horário retorna 409. Appointment Cancelado não bloqueia o slot.

`PUT /api/appointments/{id}` recebe `statusId`. Transições aceitas: Agendada para Confirmada ou Cancelada; Confirmada para Em atendimento ou Cancelada; Em atendimento para Concluída. Concluída e Cancelada são finais. Transição inválida retorna 400.

`DELETE /api/appointments/{id}` preserva o registro, altera o status para Cancelada e retorna 204. Appointment Concluída ou já Cancelada não pode ser cancelada. GET lista ou retorna `id,patientId,patientName,professionalId,professionalName,date,time,statusId,statusName,notes,created`.

### Prontuários

MedicalRecord possui somente `id`, `appointment` e `notes`. V15 converte registros históricos para texto legível antes de remover symptoms, diagnosis e prescription, mantém `notes` obrigatório e preserva `UNIQUE(id_appointment)`.

`POST /api/medical-records` recebe `appointmentId` e `notes`, ambos obrigatórios. Somente um prontuário pode existir por Appointment; duplicidade retorna 409. GET lista/consulta `id,appointmentId,notes`. PUT altera somente notes. Não existe DELETE de prontuário.

Measurement não tem schema aprovado e não foi implementado. Não existem endpoints adicionais de dashboard/relatórios.

## Erros

Formato único ApiErrorResponse: `timestamp,status,error,message,path,details`. details contém erros de validação quando aplicável.

```json
{"timestamp":"2026-10-20T21:00:00","status":404,"error":"Not Found","message":"Paciente não encontrado","path":"/api/patients/00000000-0000-0000-0000-000000000000","details":null}
```

| Código | Situação |
| --- | --- |
| 400 | Validação, JSON/UUID inválido, BusinessException |
| 401 | Autenticação ausente/inválida, login incorreto |
| 403 | Perfil sem permissão ou rota sem política aprovada |
| 404 | Recurso inexistente, quando a rota está autorizada |
| 405 | Método não suportado, quando alcança o MVC |
| 409 | Unicidade/FK do banco, Appointment ativo no slot ou segundo prontuário |
| 500 | Erro inesperado, sem detalhes internos expostos |

## CORS

`APP_CORS_ALLOWED_ORIGINS` configura `app.cors.allowed-origins`: lista de origens exatas separadas por vírgula, por exemplo `http://localhost:5173` quando essa for a origem acordada do ambiente. Vazio por padrão: nenhuma origem externa liberada. Não usar wildcard, caminho, query, fragmento ou credenciais na origem. Métodos GET/POST/PUT/DELETE/OPTIONS; headers Authorization e Content-Type; sem cookies/credentials. Preflight aceito não concede autenticação nem libera rotas bloqueadas.

## Testes

`./mvnw.cmd test` no Windows ou `./mvnw test` no Linux/macOS. Sem RUN_POSTGRES_TESTS=true, os testes condicionados a PostgreSQL são ignorados intencionalmente. Para executar todos, usar banco exclusivo, configurar DB_URL/DB_USERNAME/DB_PASSWORD e RUN_POSTGRES_TESTS=true, executar verify. Não usar banco de desenvolvimento.

SecurityHttpTest usa filtros/JWT reais e services simulados. AdministrativeControllersTest testa MVC isoladamente sem filtro, complementado pela comprovação de negação dos mesmos endpoints em SecurityHttpTest. ApiPostgresTest cobre login, BCrypt e fluxo de pacientes HTTP com persistência real. Os números e gates desta rodada estão no relatório de auditoria; contagem não representa cobertura percentual.
