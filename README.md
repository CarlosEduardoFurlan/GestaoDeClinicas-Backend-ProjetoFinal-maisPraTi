# Gestão de Clínicas — Backend

API do Sistema de Gestão de Clínicas do projeto final +praTI.

## Tecnologias
- Java / JDK 26
- Spring Boot 4.1.1
- Spring Data JPA
- Flyway
- PostgreSQL
- Maven
- Lombok

## Estrutura
O projeto possui uma única aplicação Spring Boot na raiz do repositório.

```text
src/
├── main/
│   ├── java/br/com/clinica/
│   │   ├── config/
│   │   ├── security/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/request/
│   │   ├── dto/response/
│   │   ├── mapper/
│   │   ├── exception/
│   │   └── validation/
│   └── resources/
│       ├── application.properties
│       └── db/migration/
└── test/java/br/com/clinica/
    ├── controller/
    ├── service/
    └── repository/
```

## Banco de dados
As migrations oficiais ficam em `src/main/resources/db/migration/`.
Elas devem ser a única fonte versionada da estrutura do banco.

Não crie tabelas manualmente para fazer a aplicação funcionar.

## Configuração local
Crie um banco PostgreSQL chamado `clinica` e configure as variáveis locais `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`.

A senha não deve ser colocada no repositório.

## Versionamento
Fluxo adotado:

```text
feature/* → develop → main
```

- `main`: versão estável.
- `develop`: integração.
- `feature/*`: desenvolvimento de tarefas individuais.

## Execução
Windows:

```bash
mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```
