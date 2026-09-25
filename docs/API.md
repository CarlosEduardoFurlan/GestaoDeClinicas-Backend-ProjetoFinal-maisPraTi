# API — Sistema de Gestão de Clínicas

## Execução local

- JDK 26.
- PostgreSQL configurado por DB_URL, DB_USERNAME e DB_PASSWORD.
- Endereço padrão: http://localhost:8080
- Autenticação e autorização ainda não implementadas.

## Endpoints disponíveis

| Método | Rota | Operação | Sucesso |
| --- | --- | --- | --- |
| GET | /api/patients | Listar pacientes | 200 |
| GET | /api/patients/{id} | Buscar paciente | 200 |
| POST | /api/patients | Criar paciente | 201 |
| PUT | /api/patients/{id} | Atualizar paciente | 200 |
| DELETE | /api/patients/{id} | Excluir paciente | 204 |
| GET | /api/professionals | Listar profissionais | 200 |
| GET | /api/professionals/{id} | Buscar profissional | 200 |
| POST | /api/professionals | Criar profissional | 201 |

## Validações manuais realizadas

- Listagem de pacientes sem registros: retornou [].
- Busca de paciente inexistente: retornou 404.
- Busca com ID inválido: retornou 400.
- Cadastro de paciente com JSON vazio: retornou 400.
- Listagem de profissionais sem registros: retornou [].

## Testes automatizados locais

- 16 testes unitários aprovados.
- 5 testes de integração com PostgreSQL não executados.

## Dependências para cadastro

- Paciente: requer addressId de um endereço existente.
- Profissional: requer specialtyId de uma especialidade existente.
- O cadastro de profissional recebe endereço e usuário aninhados.
## Cadastro de paciente

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
A senha é fictícia. PROFESSIONAL é um exemplo de perfil; as permissões
serão definidas na etapa de segurança.

### Comportamento

- Cria o usuário, o endereço e o profissional na mesma transação.
- Armazena a senha como hash BCrypt.
- A resposta não inclui a senha.

### Respostas esperadas

- 201: profissional criado.
- 400: dados inválidos, registro profissional duplicado ou e-mail
  do usuário já cadastrado.
- 404: especialidade não encontrada.

### Segurança pendente

O cadastro ainda precisa de autorização administrativa, pois recebe
dados de usuário e perfil.

Este exemplo documenta o contrato; o cadastro válido ainda não foi
testado manualmente nesta etapa.

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
| 404 | Recurso não encontrado |
| 405 | Método HTTP não permitido para a rota |
| 409 | Conflito de integridade, como unicidade ou vínculo com outro registro |
| 500 | Erro interno inesperado |

As respostas 401 e 403 serão implementadas na etapa de autenticação
e autorização.