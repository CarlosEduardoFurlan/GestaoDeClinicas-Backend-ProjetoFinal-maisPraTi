# 🏥 Gestão de Clínicas

#Sprints Trello https://trello.com/b/l72PNlnk/sistema-clinico

Sistema Full Stack para gerenciamento de clínicas médicas, desenvolvido como projeto final da formação de Desenvolvedor Full Stack Junior — Grupo +praTI e Codifica.

O projeto tem como objetivo transformar a modelagem de dados inicialmente planejada em uma aplicação web funcional, organizada em uma arquitetura moderna de **backend + API REST + frontend**, contemplando gerenciamento de usuários, pacientes, profissionais, especialidades, agenda, agendamentos e registros de atendimento.

> 🚧 **Status:** Em desenvolvimento

---

## 📋 Índice

* [Sobre o projeto](#-sobre-o-projeto)
* [Objetivos](#-objetivos)
* [Funcionalidades](#-funcionalidades)
* [Arquitetura](#-arquitetura)
* [Tecnologias](#-tecnologias)
* [Modelo de domínio](#-modelo-de-domínio)
* [Estrutura do Backend](#-estrutura-do-backend)
* [Estrutura do Frontend](#-estrutura-do-frontend)
* [API REST](#-api-rest)
* [Autenticação e autorização](#-autenticação-e-autorização)
* [Banco de dados](#-banco-de-dados)
* [Metodologia de desenvolvimento](#-metodologia-de-desenvolvimento)
* [Sprints](#-sprints)
* [Qualidade e testes](#-qualidade-e-testes)
* [Fluxo Git](#-fluxo-git)
* [Equipe](#-equipe)
* [Roadmap](#-roadmap)
* [Critérios de pronto](#-critérios-de-pronto)
* [Documentação](#-documentação)
* [Licença](#-licença)

---

# 🩺 Sobre o projeto

O **Gestão de Clínicas** é uma aplicação desenvolvida para centralizar e organizar processos relacionados à administração de uma clínica médica.

A solução foi planejada para permitir o gerenciamento de:

* 👤 Usuários
* 🧑‍⚕️ Profissionais
* 🧑‍🤝‍🧑 Pacientes
* 🩺 Especialidades
* 📅 Horários e agendas
* 📋 Agendamentos
* 📝 Registros de atendimento
* 🔐 Autenticação e autorização
* 📊 Dashboard e informações operacionais

O projeto parte de uma modelagem de banco de dados definida por migrations Flyway. Essas migrations precisam ser executadas e validadas em um ambiente PostgreSQL de desenvolvimento antes que o modelo seja considerado definitivamente validado.

---

# 🎯 Objetivos

## Objetivo geral

Desenvolver uma aplicação Full Stack funcional para gerenciamento de clínicas, utilizando boas práticas de engenharia de software, separação de responsabilidades, API REST, persistência de dados, autenticação, autorização e testes.

## Objetivos específicos

* Validar a modelagem do banco de dados.
* Implementar o backend utilizando Spring Boot.
* Utilizar PostgreSQL como banco de dados.
* Gerenciar alterações estruturais através do Flyway.
* Mapear o banco para entidades JPA.
* Implementar repositories, services e controllers.
* Criar DTOs para comunicação com a API.
* Implementar validações e tratamento de exceções.
* Implementar autenticação e autorização.
* Desenvolver uma interface web utilizando React.
* Integrar frontend e backend.
* Implementar testes unitários e de integração.
* Garantir responsividade e acessibilidade.
* Preparar o projeto para Docker, deploy e apresentação final.

---

# ✨ Funcionalidades

## 👤 Usuários

* Login
* Gerenciamento de sessão
* Perfis/roles
* Controle de acesso
* Autorização de recursos

## 🧑‍🤝‍🧑 Pacientes

* Cadastro de pacientes
* Listagem
* Consulta individual
* Atualização
* Exclusão
* Validação de dados

## 🧑‍⚕️ Profissionais

* Cadastro de profissionais
* Listagem
* Atualização
* Exclusão
* Associação com especialidades
* Associação com usuário

## 🩺 Especialidades

* Cadastro
* Listagem
* Atualização
* Exclusão

## 📅 Agenda

* Cadastro de horários
* Consulta de horários
* Atualização
* Exclusão
* Associação com profissional
* Validação de conflitos de horário

## 📋 Agendamentos

* Criar agendamento
* Listar agendamentos
* Atualizar agendamento
* Excluir/cancelar agendamento
* Associação entre paciente e profissional
* Controle de status

## 📝 Consultas e prontuário

* Registro do atendimento
* Associação com agendamento
* Histórico do paciente
* Consulta de registros

## 📊 Dashboard

* Indicadores do sistema
* Informações de pacientes
* Informações de profissionais
* Agendamentos
* Consultas
* Filtros e buscas

---

# 🏗️ Arquitetura

O projeto é dividido em duas aplicações principais:

```text
GestaoDeClinicas-ProjetoFinal-maisPraTi/
│
├── backend/
│
└── frontend/
```

A comunicação entre as aplicações ocorre através de uma **API REST**.

```text
┌──────────────────────┐
│      FRONTEND        │
│        React         │
└──────────┬───────────┘
           │
           │ HTTP / REST
           ▼
┌──────────────────────┐
│       BACKEND        │
│     Spring Boot      │
│                      │
│ Controllers          │
│ Services             │
│ Repositories         │
│ Entities / DTOs      │
│ Security / Validation│
└──────────┬───────────┘
           │
           │ JPA
           ▼
┌──────────────────────┐
│      PostgreSQL      │
│                      │
│      Flyway          │
│     Migrations       │
└──────────────────────┘
```

---

# 🛠️ Tecnologias

## Backend

* ☕ Java
* 🍃 Spring Boot
* 🗃️ Spring Data JPA
* 🐘 PostgreSQL
* 🔄 Flyway
* 🔐 Spring Security
* 🧪 Testes unitários e de integração

## Frontend

* ⚛️ React
* JavaScript/TypeScript conforme a implementação do projeto
* Consumo de API REST
* Componentização
* Gerenciamento de rotas
* Responsividade
* Acessibilidade

## Desenvolvimento

* Git
* GitHub
* Pull Requests
* Code Review
* Docker
* Metodologia baseada em Sprints

> As versões específicas das ferramentas devem ser consultadas nos arquivos de configuração do projeto.

---

# 🗄️ Banco de dados

O banco de dados utiliza **PostgreSQL** e sua estrutura é controlada através do **Flyway**.

As migrations inicialmente previstas são:

| Migration | Entidade      | Responsabilidade                    |
| --------- | ------------- | ----------------------------------- |
| V1        | User          | Credenciais, perfil e identificação |
| V2        | Address       | Endereço                            |
| V3        | Specialty     | Especialidades                      |
| V4        | Patient       | Dados dos pacientes                 |
| V5        | Professional  | Profissionais e vínculos            |
| V6        | Status        | Status dos agendamentos             |
| V7        | Schedule      | Horários dos profissionais          |
| V8        | Appointment   | Agendamentos                        |
| V9        | MedicalRecord | Registro do atendimento             |

A documentação determina que essas migrations sejam **revisadas, executadas e testadas em um PostgreSQL de desenvolvimento** antes da validação definitiva do modelo.

---

# 🔗 Modelo de domínio

Os principais relacionamentos previstos são:

```text
Patient
   │
   └──── 1:N ──── Appointment

Professional
   │
   ├──── 1:N ──── Appointment
   │
   └──── 1:N ──── Schedule

Specialty
   │
   └──── 1:N ──── Professional

Appointment
   │
   └──── ? ────── MedicalRecord

User
   │
   └──── ? ────── Professional
```

Os relacionamentos marcados como `?` precisam de decisão de domínio da equipe antes de serem considerados definitivos.

---

# 🧩 Estrutura do Backend

A arquitetura recomendada para o backend segue uma organização por responsabilidades:

```text
backend/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── br/com/clinica/
│   │   │       │
│   │   │       ├── config/
│   │   │       ├── security/
│   │   │       │
│   │   │       ├── controller/
│   │   │       │   ├── AuthController.java
│   │   │       │   ├── UserController.java
│   │   │       │   ├── PatientController.java
│   │   │       │   ├── ProfessionalController.java
│   │   │       │   ├── SpecialtyController.java
│   │   │       │   ├── ScheduleController.java
│   │   │       │   ├── AppointmentController.java
│   │   │       │   └── MedicalRecordController.java
│   │   │       │
│   │   │       ├── service/
│   │   │       ├── repository/
│   │   │       ├── entity/
│   │   │       ├── dto/
│   │   │       │   ├── request/
│   │   │       │   └── response/
│   │   │       ├── mapper/
│   │   │       ├── exception/
│   │   │       └── validation/
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/
│   │           └── migration/
│   │
│   └── test/
│
├── pom.xml
└── README.md
```

A estrutura proposta no planejamento busca separar responsabilidades entre configuração, segurança, controllers, services, repositories, entidades, DTOs, mappers, exceptions e validações.

---

# ⚛️ Estrutura do Frontend

A estrutura planejada para o React é:

```text
frontend/
│
├── src/
│   ├── assets/
│   │
│   ├── components/
│   │   ├── Button/
│   │   ├── Input/
│   │   ├── Modal/
│   │   ├── Table/
│   │   └── Sidebar/
│   │
│   ├── layouts/
│   │
│   ├── pages/
│   │   ├── Login/
│   │   ├── Dashboard/
│   │   ├── Patients/
│   │   ├── Professionals/
│   │   ├── Specialties/
│   │   ├── Schedule/
│   │   ├── Appointments/
│   │   ├── MedicalRecords/
│   │   └── Profile/
│   │
│   ├── services/
│   ├── contexts/
│   ├── hooks/
│   ├── routes/
│   ├── types/
│   └── utils/
│
├── App.tsx
├── main.tsx
└── package.json
```

Essa organização segue a estrutura recomendada na documentação do projeto.

---

# 🔌 API REST

Os endpoints iniciais previstos para a aplicação incluem:

## Autenticação

```http
POST /api/auth/login
```

## Pacientes

```http
GET    /api/patients
POST   /api/patients
GET    /api/patients/{id}
PUT    /api/patients/{id}
DELETE /api/patients/{id}
```

## Profissionais

```http
GET    /api/professionals
POST   /api/professionals
PUT    /api/professionals/{id}
DELETE /api/professionals/{id}
```

## Especialidades

```http
GET    /api/specialties
POST   /api/specialties
PUT    /api/specialties/{id}
DELETE /api/specialties/{id}
```

## Agenda

```http
GET  /api/schedules
POST /api/schedules
```

## Agendamentos

```http
GET    /api/appointments
POST   /api/appointments
PUT    /api/appointments/{id}
DELETE /api/appointments/{id}
```

## Registros médicos

```http
GET  /api/medical-records/{id}
POST /api/medical-records
```

Esses endpoints são uma referência inicial. Antes da implementação definitiva, devem ser definidos os payloads, filtros, permissões e códigos HTTP.

---

# 🔐 Autenticação e autorização

A aplicação deverá possuir mecanismos de:

* Login
* Gerenciamento de sessão
* Roles/perfis
* Proteção de endpoints
* Proteção de rotas do frontend
* Controle de permissões

A segurança também contempla:

* Utilização de hash adequado para senhas.
* Não versionar secrets.
* Validar entradas recebidas pelo backend.
* Testar autenticação.
* Testar autorização.

A documentação destaca que o campo de senha originalmente planejado deve ser avaliado antes da implementação da autenticação real para garantir espaço suficiente para um hash seguro.

---

# 🗓️ Metodologia de desenvolvimento

O projeto será desenvolvido utilizando uma abordagem baseada em **Sprints**, com entregas incrementais.

## Sprint 1 — Fundação

Objetivo: estabelecer a infraestrutura e arquitetura inicial.

Principais atividades:

* Revisar migrations.
* Criar PostgreSQL de desenvolvimento.
* Executar Flyway.
* Validar tabelas e relacionamentos.
* Criar novas migrations quando necessário.
* Criar entidades JPA.
* Criar repositories.
* Configurar conexão Spring Boot + PostgreSQL.
* Definir DTOs, services e exceptions.
* Criar estrutura inicial do React.
* Integrar a base na `develop`.

Critério de conclusão:

> Banco criado corretamente pelas migrations, backend conectado ao PostgreSQL, projeto compilando e estrutura base integrada na `develop`.

---

# 🚀 Sprint 2 — Usuários e Pacientes

Principais entregas:

* Login inicial.
* Roles/perfis.
* CRUD de pacientes.
* Validações.
* Telas de pacientes.
* Primeiros testes.

---

# 📅 Sprint 3 — Profissionais, Especialidades e Agenda

Principais entregas:

* CRUD de especialidades.
* CRUD de profissionais.
* Agenda.
* Agendamentos.
* Regras de conflito de horário.
* Integração frontend/backend.

---

# 📊 Sprint 4 — Consultas e Dashboard

Principais entregas:

* Registro de consultas.
* Histórico do paciente.
* Dashboard.
* Filtros.
* Buscas.
* Ajustes de autorização.
* Testes dos fluxos principais.

---

# 🧪 Qualidade e testes

A qualidade será trabalhada durante todo o desenvolvimento.

## Testes previstos

* Testes unitários.
* Testes de integração.
* Testes de endpoints.
* Testes de autenticação.
* Testes de autorização.
* Testes dos CRUDs.
* Testes dos fluxos principais.
* Testes de responsividade.
* Testes de acessibilidade.

## Meta de cobertura

O projeto possui como objetivo atingir **no mínimo 70% de cobertura de testes**, conforme requisito definido para o curso.

---

# 🔄 Fluxo Git

O desenvolvimento seguirá o fluxo:

```text
feature
   │
   ▼
develop
   │
   ▼
testes
   │
   ▼
main
```

Cada funcionalidade deve ser desenvolvida em uma branch própria.

Exemplos:

```text
feature/auth-login
feature/patients-crud
feature/professionals-crud
feature/appointments
feature/dashboard
```

Depois:

```text
feature/* → Pull Request → develop
```

Após testes e validação:

```text
develop → main
```

---

# 🔍 Code Review

Antes de considerar uma funcionalidade concluída, o código deve passar por revisão.

### Checklist

* [ ] Código implementado
* [ ] Testes executados
* [ ] Sem erro crítico conhecido
* [ ] Branch criada a partir da `develop`
* [ ] Commits claros
* [ ] Pull Request aberto
* [ ] Code Review realizado
* [ ] Correções aplicadas
* [ ] Merge realizado na `develop`
* [ ] Integração validada

Essa sequência corresponde à definição de pronto estabelecida no planejamento do projeto.

---

# 👥 Organização da equipe

A equipe é composta por **8 integrantes**, com responsabilidades principais distribuídas entre os seguintes focos:

| Integrante | Responsabilidade principal             |
| ---------- | -------------------------------------- |
| 1          | Arquitetura + integração geral         |
| 2          | Segurança + usuários                   |
| 3          | Pacientes                              |
| 4          | Profissionais + especialidades         |
| 5          | Agenda + agendamentos                  |
| 6          | Consultas + prontuário                 |
| 7          | Testes + qualidade                     |
| 8          | Front-end base + DevOps + documentação |

As responsabilidades são principais, não exclusivas. A integração entre backend e frontend deve ser coordenada através de contratos de API previamente definidos.

---

# 📈 Roadmap

```text
┌────────────────────────────────────────────┐
│              ROADMAP DO PROJETO            │
└────────────────────────────────────────────┘

AGOSTO
│
├── Validação das migrations
├── PostgreSQL
├── Flyway
├── Entidades
├── Repositories
├── Arquitetura
└── React base
        │
        ▼
SETEMBRO
│
├── Usuários
├── Pacientes
├── Profissionais
├── Especialidades
├── Agenda
├── Agendamentos
└── Integração
        │
        ▼
OUTUBRO
│
├── Consultas
├── Prontuário
├── Dashboard
├── Testes
├── Segurança
├── Responsividade
├── Docker
├── Deploy
├── Documentação
└── Correções
        │
        ▼
ÚLTIMA SEMANA DE OUTUBRO
│
└── Congelamento da versão final
        │
        ▼
NOVEMBRO
│
└── Apresentação e demonstração
```

O cronograma oficial prevê fundação em agosto, desenvolvimento das principais funcionalidades em setembro e finalização, testes, segurança, Docker, deploy e documentação em outubro.

---

# 📌 Critérios de pronto

Uma funcionalidade somente será considerada concluída quando:

```text
Código
  ↓
Testes
  ↓
Sem erros críticos
  ↓
Branch
  ↓
Commit
  ↓
Pull Request
  ↓
Code Review
  ↓
Correções
  ↓
Merge na develop
  ↓
Integração validada
```

O objetivo é garantir que nenhuma funcionalidade seja considerada concluída apenas porque o código foi escrito.

---

# 📚 Documentação do projeto

A documentação do projeto contempla:

* Modelagem do banco de dados.
* Migrations Flyway.
* Arquitetura do backend.
* Arquitetura do frontend.
* Contratos de API.
* Regras de negócio.
* Estratégia de testes.
* Organização das Sprints.
* Fluxo Git.
* Critérios de pronto.
* Estratégia de deploy.
* Documentação para apresentação.

---

# 🚀 Próximos passos

A equipe deve seguir esta ordem:

1. Revisar as migrations existentes.
2. Criar o PostgreSQL de desenvolvimento.
3. Executar as migrations com Flyway.
4. Validar tabelas, chaves, relacionamentos e constraints.
5. Corrigir problemas através de novas migrations.
6. Mapear as tabelas para entidades JPA.
7. Criar repositories.
8. Criar DTOs, services, validações e exceptions.
9. Criar controllers e endpoints REST.
10. Implementar autenticação e autorização.
11. Testar a API.
12. Criar a estrutura React.
13. Integrar frontend e backend.
14. Executar testes.
15. Configurar Docker e deploy.
16. Finalizar documentação.
17. Realizar ensaio da apresentação.

---

# 🎓 Contexto acadêmico

Este projeto faz parte da formação:

**Desenvolvedor Full Stack Junior — Grupo +praTI e Codifica — 2026**

O desenvolvimento foi estruturado para aplicar conceitos de:

* Engenharia de software
* Desenvolvimento Full Stack
* APIs REST
* Banco de dados relacionais
* Arquitetura de software
* Orientação a objetos
* Segurança
* Testes
* Git e GitHub
* Trabalho colaborativo
* Metodologias ágeis
* DevOps

---

# 📌 Status do projeto

```text
🟡 EM DESENVOLVIMENTO

Backend       🟡 Em desenvolvimento
Frontend      🟡 Em desenvolvimento
Database      🟡 Em validação
API           🟡 Em desenvolvimento
Autenticação  🟡 Planejada / em desenvolvimento
Testes        🟡 Em desenvolvimento
Docker        ⏳ Planejado
Deploy        ⏳ Planejado
Documentação  🟡 Em desenvolvimento
```

---

# 📄 Licença

Projeto desenvolvido em contexto acadêmico para a formação **Desenvolvedor Full Stack Junior — Grupo +praTI e Codifica — 2026**.

Consulte os arquivos do repositório para informações específicas sobre licenciamento e utilização do código.
