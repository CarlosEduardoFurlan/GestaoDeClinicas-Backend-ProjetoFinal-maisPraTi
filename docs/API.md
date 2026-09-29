# API — Sistema de Gestão de Clínicas

## Execução local

- JDK 26.
- PostgreSQL configurado por DB_URL, DB_USERNAME e DB_PASSWORD.
- Endereço padrão: http://localhost:8080
- JWT_SECRET configurada no ambiente com uma chave aleatória de pelo
  menos 32 bytes, representada em Base64.
- Login disponível em POST /api/auth/login.
- Autenticação por JWT no cabeçalho Authorization: Bearer <token>.
- POST /api/professionals exige o perfil ADMIN.
- POST /api/patients exige o perfil ADMIN ou RECEPTIONIST.
- GET /api/patients exige o perfil ADMIN ou RECEPTIONIST.
- Consulta por ID e atualização de pacientes, além de listagem e consulta
  por ID de profissionais, exigem ADMIN ou RECEPTIONIST.
- Rotas sem permissão definida permanecem bloqueadas provisoriamente.

## Endpoints disponíveis

| Método | Rota | Operação | Sucesso previsto | Acesso atual |
| --- | --- | --- | --- | --- |
| POST | /api/auth/login | Autenticar usuário | 200 | Público |
| GET | /api/patients | Listar pacientes | 200 | JWT com perfil ADMIN ou RECEPTIONIST |
| GET | /api/patients/{id} | Buscar paciente | 200 | JWT com perfil ADMIN ou RECEPTIONIST |
| POST | /api/patients | Criar paciente | 201 | JWT com perfil ADMIN ou RECEPTIONIST |
| PUT | /api/patients/{id} | Atualizar paciente | 200 | JWT com perfil ADMIN ou RECEPTIONIST |
| DELETE | /api/patients/{id} | Excluir paciente | 204 | Bloqueado provisoriamente |
| GET | /api/professionals | Listar profissionais | 200 | JWT com perfil ADMIN ou RECEPTIONIST |
| GET | /api/professionals/{id} | Buscar profissional | 200 | JWT com perfil ADMIN ou RECEPTIONIST |
| POST | /api/professionals | Criar profissional | 201 | JWT com perfil ADMIN |

## Login

POST /api/auth/login
Content-Type: application/json

Não exige token de autenticação.

### Exemplo de entrada

```json
{
  "email": "usuario@example.com",
  "password": "senha-ficticia"
}
```

Os dados são ilustrativos. Use as credenciais de um usuário cadastrado.

### Exemplo de resposta — 200 OK

```json
{
  "accessToken": "JWT_ILUSTRATIVO",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

expiresIn representa a duração do token em segundos. O exemplo
considera a configuração atual de 15 minutos.

Nas requisições protegidas, envie:
Authorization: Bearer <accessToken>

### Respostas de erro

- 400: campos obrigatórios ausentes ou formato de e-mail inválido.
- 401: e-mail ou senha inválidos.

O perfil é obtido do usuário cadastrado no banco, não do corpo do login.

## Validações manuais anteriores à implementação da segurança

Os resultados abaixo foram obtidos antes da configuração de JWT
e das restrições de acesso atuais.

- Listagem de pacientes sem registros: retornou [].
- Busca de paciente inexistente: retornou 404.
- Busca com ID inválido: retornou 400.
- Cadastro de paciente com JSON vazio: retornou 400.
- Listagem de profissionais sem registros: retornou [].

## Testes automatizados — execução anterior à segurança

Os números abaixo correspondem à execução anterior à implementação
da segurança. Os resultados atuais estão na seção de testes automatizados
de autenticação ao final deste documento.

- 16 testes unitários aprovados.
- 5 testes de integração com PostgreSQL não executados.

## Dependências para cadastro

- Paciente: requer addressId de um endereço existente.
- Profissional: requer specialtyId de uma especialidade existente.
- O cadastro de profissional recebe endereço e usuário aninhados.
## Cadastro de paciente

Exige JWT válido com perfil ADMIN ou RECEPTIONIST. Sem autenticação,
retorna 401; com perfil PROFESSIONAL, retorna 403.
A listagem GET /api/patients também exige ADMIN ou RECEPTIONIST.
A consulta GET /api/patients/{id} também exige ADMIN ou RECEPTIONIST: retorna 200 quando encontrado, 404 quando inexistente e 400 para UUID inválido. PROFESSIONAL recebe 403; sem autenticação, 401. A atualização por PUT também exige ADMIN ou RECEPTIONIST. A exclusão continua bloqueada.

POST /api/patients
Content-Type: application/json

Exemplo de entrada:

```json
{
  "name": "Maria Silva",
  "cpf": "12345678901",
  "dateBirth": "1990-05-20",
  "telephone": "6533333333",
  "cellPhone": "65999999999",
  "email": "maria@example.com",
  "gender": "F",
  "addressId": "00000000-0000-0000-0000-000000000000"
}
```

O addressId acima é ilustrativo: substitua pelo UUID de um endereço existente.

### Campos obrigatórios

name, cpf, dateBirth, cellPhone, gender e addressId.

### Respostas esperadas

- 201: paciente criado; retorna os dados e o ID gerado.
- 400: dados inválidos ou CPF/e-mail já cadastrado.
- 404: endereço informado não encontrado.

Este exemplo documenta o contrato; o cadastro válido ainda não foi testado manualmente nesta etapa.

## Cadastro de profissional

POST /api/professionals
Content-Type: application/json

Exemplo de entrada:

```json
{
  "name": "Ana Silva",
  "register": "CRM-12345",
  "telephone": "6533333333",
  "cellPhone": "65999999999",
  "email": "ana@example.com",
  "specialtyId": "00000000-0000-0000-0000-000000000000",
  "address": {
    "address": "Rua das Flores",
    "number": 100,
    "complement": "Sala 2",
    "district": "Centro",
    "city": "Cuiabá",
    "uf": "MT",
    "zipCode": "78000-000"
  },
  "user": {
    "name": "Ana Silva",
    "email": "ana@example.com",
    "password": "senha-ficticia",
    "profile": "PROFESSIONAL"
  }
}
```

Substitua specialtyId pelo UUID de uma especialidade existente.
A senha é fictícia. user.profile aceita exatamente ADMIN, RECEPTIONIST ou PROFESSIONAL. Valor ausente, vazio, desconhecido, em minúsculas ou com espaços retorna 400 antes do service. O cadastro continua exclusivo de ADMIN.

### Comportamento

- Cria o usuário, o endereço e o profissional na mesma transação.
- Armazena a senha como hash BCrypt.
- A resposta não inclui a senha.

### Respostas esperadas

- 201: profissional criado.
- 400: dados inválidos, registro profissional duplicado ou e-mail
  do usuário já cadastrado.
- 404: especialidade não encontrada.

### Segurança

POST /api/professionals exige um token JWT válido de usuário com
perfil ADMIN, enviado no cabeçalho Authorization: Bearer <token>.

- 401: autenticação ausente ou token inválido/expirado.
- 403: usuário autenticado sem o perfil ADMIN.

## Respostas de erro

A API utiliza os campos:

- timestamp: data e hora do erro.
- status: código HTTP.
- error: nome do status HTTP.
- message: descrição do problema.
- path: rota acessada.
- details: lista de erros de validação, quando aplicável.

Exemplo ilustrativo para paciente inexistente:

```json
{
  "timestamp": "2026-09-25T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Paciente não encontrado: 00000000-0000-0000-0000-000000000000",
  "path": "/api/patients/00000000-0000-0000-0000-000000000000",
  "details": null
}
```

### Códigos utilizados

| Código | Significado |
| --- | --- |
| 400 | Dados inválidos, JSON malformado, parâmetro inválido ou regra de negócio violada |
| 401 | Credenciais inválidas, autenticação ausente ou token inválido/expirado |
| 403 | Usuário autenticado sem permissão para acessar o recurso |
| 404 | Recurso não encontrado |
| 405 | Método HTTP não permitido para a rota |
| 409 | Conflito de integridade, como unicidade ou vínculo com outro registro |
| 500 | Erro interno inesperado |

## Autenticação — validações manuais

- Login de administrador com credenciais válidas: 200, retornando
  accessToken, tokenType e expiresIn.
- Login com corpo vazio: 400.
- Login com e-mail inexistente: 401.
- Login de usuário existente com senha incorreta: 401.
- Acesso a rota protegida sem token: 401.
- Requisição com token inválido: 401.
- Requisição com token expirado: 401.
- POST /api/professionals com token ADMIN e corpo vazio: 400,
  indicando que passou pela autorização e chegou à validação.
- POST /api/professionals com token RECEPTIONIST: 403.
- GET /api/patients com token válido: retornou 403 no teste anterior
  à liberação da listagem. Atualmente ADMIN e RECEPTIONIST podem listar;
  PROFESSIONAL continua recebendo 403.

O cadastro válido de profissional ainda não foi verificado manualmente.
Ele foi validado pelo teste automatizado de integração com PostgreSQL.
Os resultados acima são testes manuais.

## Testes automatizados de autenticação e integração

Na etapa anterior, a suíte foi executada com Java 26, Spring Boot 4.1.1 e PostgreSQL 18.3
temporário e isolado: **69 testes aprovados, sem falhas ou ignorados**.
O PostgreSQL temporário foi encerrado após a execução. O banco de
desenvolvimento e sua chave JWT não foram utilizados.

Após liberar POST /api/patients para ADMIN e RECEPTIONIST (28/09/2026),
a execução sem banco teve **70 testes aprovados e 5 de PostgreSQL
ignorados**, sem falhas. Foram acrescentados seis casos HTTP: criação
permitida e validação de campos para cada perfil autorizado, rejeição
de PROFESSIONAL e rejeição de requisição sem token. A criação nesses
casos usa PatientService simulado; os filtros e o JWT são reais.

Após liberar também GET /api/patients, a execução sem banco teve
**74 testes aprovados e 5 de PostgreSQL ignorados**, sem falhas.
Os quatro novos casos verificam a listagem para ADMIN e RECEPTIONIST,
a rejeição de PROFESSIONAL e a exigência de autenticação.

| Classe | Casos executados | Verificações principais |
| --- | --- | --- |
| AuthServiceTest | 2 | Falha não emite token; sucesso usa o resultado autenticado |
| AuthenticationTest | 14 | Busca por e-mail, três perfis aprovados, perfis inválidos, BCrypt real, senha incorreta e remoção das credenciais |
| JwtServiceTest | 12 | Token válido, assinatura com outra chave, conteúdo adulterado, expiração, validade futura, emissor, formato, conversão de perfis e configuração inválida |
| SecurityHttpTest | 66 | Login, erros JSON 400/401/403, JWT real, ausência de sessão; cadastro de profissionais somente ADMIN; cadastro e listagem de pacientes para ADMIN ou RECEPTIONIST |
| PatientServiceTest | 11 | Regras de pacientes |
| ProfessionalServiceTest | 5 | Regras de profissionais |
| ApiPostgresTest | 5 | Migrations, JPA, regras persistidas, cadastro com JWT ADMIN, BCrypt armazenado e login do usuário criado |

As contagens incluem as execuções de testes parametrizados.

AuthServiceTest usa dependências simuladas. AuthenticationTest usa
repositório simulado e autenticação/BCrypt reais. SecurityHttpTest usa
controllers, filtros, autenticação e JWT reais, simulando apenas o
repositório de usuários e os serviços de pacientes/profissionais.
Os testes JWT geram chaves aleatórias exclusivas para a execução.

ApiPostgresTest mantém os filtros habilitados. CRUD e duplicidades de pacientes
são verificados pelo PatientService com persistência real. As listagens HTTP
de pacientes e profissionais com ADMIN também passaram no PostgreSQL.
Cadastro de profissional, BCrypt persistido e login do usuário criado foram
validados. Consulta por ID e edição têm cobertura HTTP com serviços simulados;
isso não equivale a testar todos esses fluxos HTTP de ponta a ponta no banco.

### Como executar

Sem banco, pelo Maven Wrapper:

```text
.\mvnw.cmd test
```

Sem RUN_POSTGRES_TESTS=true, são esperados 110 testes aprovados e
5 testes de PostgreSQL ignorados.

Para executar também ApiPostgresTest, configure RUN_POSTGRES_TESTS=true
e DB_URL, DB_USERNAME e DB_PASSWORD apontando para um banco exclusivo
de testes com schema inicialmente vazio. Flyway aplica as migrations;
os dados inseridos pelos testes são revertidos por transação.
A chave JWT de integração é gerada pelo teste, sem exigir JWT_SECRET.
As variáveis devem estar na configuração de execução do Maven/testes,
não apenas na configuração de ClinicaApplication.

### Limites atuais

Os testes cobrem as regras aprovadas: login público, cadastro de
profissionais exclusivo de ADMIN e cadastro/listagem/consulta por ID/edição de pacientes para ADMIN
ou RECEPTIONIST; a listagem e a consulta por ID de profissionais também são permitidas a esses dois perfis. As demais rotas permanecem bloqueadas.
Permissões futuras e restrições por propriedade dos registros precisarão
de novos testes quando seus contratos forem definidos.
O total de testes não representa um percentual de cobertura.

### Consulta por ID — validação em 28/09/2026

GET /api/patients/{id} liberado para ADMIN e RECEPTIONIST. Nove casos HTTP adicionais verificam os dois perfis, resposta 404, UUID inválido, 401/403 e manutenção do bloqueio de PUT/DELETE. PatientService simulado; filtros e JWT reais.
Resultado: **83 aprovados, 5 de PostgreSQL ignorados, zero falhas** (88 casos totais; BUILD SUCCESS). O banco não foi utilizado nesta rodada.

### Atualização de paciente — PUT /api/patients/{id}

Exige ADMIN ou RECEPTIONIST. PROFESSIONAL recebe 403; sem token, 401.
O corpo contém name, dateBirth, telephone, cellPhone, email, gender e addressId,
com as mesmas validações desses campos no cadastro. CPF não é alterado.
Retorna 200 com PatientResponse; 400 para dados inválidos/e-mail duplicado;
404 para paciente ou endereço inexistente. DELETE permanece bloqueado.

Validação em 28/09/2026: seis novos casos HTTP verificam sucesso e corpo
inválido para ambos os perfis, rejeição de PROFESSIONAL e ausência de token.
Filtros/JWT reais e PatientService simulado.
Suíte: **89 aprovados, 5 PostgreSQL ignorados, zero falhas**; 94 casos totais,
45 em SecurityHttpTest. BUILD SUCCESS. Banco não utilizado nesta rodada.

Decisão sobre DELETE /api/patients/{id}: permanece bloqueado até alinhar com o integrante 2 a preservação do histórico. O serviço atual faz exclusão física; a política definitiva ainda está pendente.

### Listagem de profissionais — GET /api/professionals

Exige ADMIN ou RECEPTIONIST; retorna 200 com a lista de ProfessionalResponseDTO.
PROFESSIONAL recebe 403; sem token, 401. Consulta por ID também exige ADMIN ou RECEPTIONIST.
Quatro novos casos HTTP cobrem esses acessos, com filtros/JWT reais e service simulado.

Execução em 28/09/2026: **93 aprovados, 5 PostgreSQL ignorados, zero falhas**;
98 casos totais, 49 em SecurityHttpTest; BUILD SUCCESS.
ApiPostgresTest foi ajustado para verificar a listagem com ADMIN e o usuário
do profissional criado. Essa alteração passou na execução com banco registrada abaixo.
### Consulta de profissional — GET /api/professionals/{id}

Exige ADMIN ou RECEPTIONIST. Retorna 200 com ProfessionalResponseDTO;
404 para profissional inexistente e 400 para UUID inválido.
PROFESSIONAL recebe 403; sem token, 401.
Sete novos casos HTTP verificam esses comportamentos com service simulado,
controllers, filtros e JWT reais.

Execução em 28/09/2026: **100 aprovados, 5 PostgreSQL ignorados, zero falhas**;
105 casos totais, 56 em SecurityHttpTest; BUILD SUCCESS.
A integração atualizada foi executada conforme registro abaixo. DELETE de pacientes e acesso por vínculo continuam pendentes.

### Validação integrada final desta rodada — 28/09/2026

**105 testes aprovados, zero falhas, zero erros e zero ignorados. BUILD SUCCESS.**

Executada com RUN_POSTGRES_TESTS=true em PostgreSQL temporário exclusivo,
acessível apenas em 127.0.0.1:55439, com configuração de teste separada.
Flyway aplicou V1–V13 em banco vazio e o contexto JPA iniciou com validação do schema.
O servidor temporário foi encerrado após os testes. O banco de desenvolvimento
e as configurações locais com credenciais não foram utilizados.

Isso valida a suíte existente, não conclui os módulos dependentes dos colegas.
A validação HTTP de profile foi implementada na etapa seguinte, descrita abaixo.

### Validação de perfil no cadastro — 28/09/2026

UserRequestDTO mantém profile como String e agora usa @Pattern com
ADMIN|RECEPTIONIST|PROFESSIONAL, além de @NotBlank.
A validação aninhada já existente em ProfessionalRequestDTO rejeita os valores
inválidos com 400 antes de chamar ProfessionalService. Não altera registros antigos,
schema nem valida chamadas diretas ao service fora do fluxo HTTP com @Valid.
O futuro UserService deve preservar esse contrato.

Dez novos casos HTTP: os três perfis aceitos quando enviados por ADMIN e sete
entradas inválidas rejeitadas (null, vazio, ROOT, admin, ROLE_ADMIN, espaços e valor com espaços).
Última execução sem banco: **110 aprovados, 5 PostgreSQL ignorados, zero falhas**,
115 casos totais; BUILD SUCCESS. A execução integrada anterior de 105 casos
permanece histórica; não afirmar que os 115 foram executados com banco.

### Empacotamento — 28/09/2026

Maven package concluído com BUILD SUCCESS, Java 26:
110 testes aprovados, 5 PostgreSQL ignorados, zero falhas.
Artefato gerado: target/clinica-api-0.0.1-SNAPSHOT.jar,
reempacotado pelo plugin Spring Boot com suas dependências.

A execução do JAR não foi realizada nesta etapa. A inicialização e a configuração
do ambiente de entrega continuam pendentes. O resultado integrado anterior de
105 testes precede a validação de profile; não representa os 115 casos atuais.

### Inicialização do JAR — 29/09/2026

JAR executável iniciado com Java 26 e PostgreSQL 18 temporário exclusivo.
Flyway aplicou V1–V13 em banco vazio e o JPA iniciou com ddl-auto=validate.
GET /api/patients sem token retornou 401; POST /api/auth/login com {} retornou 400.
Configuração externa isolada e chave JWT aleatória de teste, sem usar o banco de
desenvolvimento. Aplicação e PostgreSQL foram encerrados ao final.
Essa verificação não representa uma nova execução da suíte nem valida o ambiente de entrega.

### Dependências pendentes

Validação para publicação em 29/09/2026: Maven verify concluído com BUILD SUCCESS,
110 testes aprovados, 5 de PostgreSQL ignorados, zero falhas e zero erros.
Executado com configuração isolada; pacote executável gerado novamente.

- Integrante 1: services/contratos de usuários e especialidades, atualização e
  exclusão de profissionais; preservar a validação de perfil e definir a política
  de alteração de e-mail, usado atualmente como subject do JWT.
- Integrante 2: alinhar preservação do histórico e exclusão de pacientes;
  DELETE permanece bloqueado até essa definição.
- Integrante 3: services/contratos de agenda e agendamentos, incluindo conflitos,
  cancelamento e vínculos necessários para acesso aos próprios registros.
- Integração: definir provisionamento do primeiro ADMIN, origem do frontend/CORS
  e configuração do ambiente de entrega.

Esta entrega cobre autenticação e permissões aprovadas; não conclui toda a Sprint 2.
