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

O `.env.example` é apenas referência: exporte as variáveis no terminal ou configure-as na IDE; o Spring Boot não lê `.env` automaticamente. Use JDK 26 e o Maven Wrapper incluído.

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

## Testes e integração

Execute `./mvnw test` (Windows: `./mvnw.cmd test`) para testes unitários. Para incluir os testes de API, migrations e JPA, use um **banco PostgreSQL exclusivo de testes**, configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` e `RUN_POSTGRES_TESTS=true`, e execute `./mvnw verify`.

O GitHub Actions executa essa suíte com PostgreSQL descartável. A base disponível, os contratos e as pendências por sprint estão em [Base para o integrante 4](docs/BASE_PARA_INTEGRANTE_4.md).
