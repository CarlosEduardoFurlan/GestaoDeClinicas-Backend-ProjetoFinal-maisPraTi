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

Configure também `JWT_SECRET` com pelo menos 32 bytes aleatórios em Base64. Nunca versione essa chave. O login é `POST /api/auth/login`; o primeiro ADMIN é criado manualmente conforme [bootstrap seguro](docs/BOOTSTRAP_ADMIN.md). Não existe cadastro público.

Para CORS, a feature de preparação aceita `APP_CORS_ALLOWED_ORIGINS` com origens exatas separadas por vírgula. Vazio por padrão; não usar `*`. Configure apenas a origem acordada do frontend no ambiente. Permissões da API continuam independentes do CORS.

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

O GitHub Actions executa essa suíte com PostgreSQL descartável. Consulte os [contratos e permissões da API](docs/API.md) e o [relatório de auditoria](docs/AUDITORIA_SPRINT2.md). A Sprint 2 ainda possui decisões pendentes; compilação ou CI verde não significam conclusão do backend.
