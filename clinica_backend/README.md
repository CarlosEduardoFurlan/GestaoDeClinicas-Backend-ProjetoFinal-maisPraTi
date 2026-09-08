# Clínica API

Backend do Sistema de Gestão de Clínicas.

## Stack
- Java
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- Flyway
- PostgreSQL
- Lombok
- Maven

## Estrutura
O projeto está organizado para evoluir por camadas:
`controller`, `service`, `repository`, `entity`, `dto`, `mapper`,
`exception`, `validation`, `config` e `security`.

## Banco de dados
As migrations atuais estão em:
`src/main/resources/db/migration/`

As migrations representam o modelo planejado do banco. Antes de desenvolver as
entidades JPA, execute-as em um PostgreSQL local e valide o resultado.

## Configuração local
1. Crie o banco `clinica` no PostgreSQL.
2. Copie `.env.example` para `.env` somente para seu ambiente local, se usar uma
   ferramenta que carregue `.env`; nunca versione o arquivo `.env`.
3. Ou configure as variáveis `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` no ambiente da IDE.
4. Execute `./mvnw spring-boot:run` (Linux/macOS) ou `mvnw.cmd spring-boot:run` (Windows).

## Branching
- `main`: versão estável.
- `develop`: integração.
- `feature/*`: tarefas individuais.
