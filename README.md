# +PraClinic — Backend

API REST de gestão clínica com autenticação, cadastros, disponibilidade, consultas e prontuários.

## Tecnologias e pré-requisitos

Java 26, Spring Boot 4.1.1, Spring Security/JWT, Spring Data JPA, PostgreSQL 18 e Flyway. Use JDK 26, PostgreSQL e o Maven Wrapper incluído; Docker é opcional para execução em container.

## Configuração

Configure no ambiente do processo `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` e `JWT_SECRET`. A chave JWT deve representar pelo menos 32 bytes aleatórios reais em Base64. O Spring não lê `.env` automaticamente. Nunca versione credenciais.

`APP_CORS_ALLOWED_ORIGINS` recebe origens exatas separadas por vírgula. Para o frontend local, configure `http://localhost:3000` somente no backend local. `PORT` é opcional localmente; o padrão é 8080. Flyway aplica as migrations e Hibernate valida o schema ao iniciar.

## Executar

Na raiz, com as variáveis configuradas:

```sh
./mvnw spring-boot:run
```

Windows: `mvnw.cmd spring-boot:run`. API local: `http://localhost:8080/api` sem PORT definida. Não há cadastro público ou credencial padrão; o primeiro ADMIN exige provisionamento manual seguro.

## Testar

Configure as variáveis de um PostgreSQL 18 **exclusivo para testes** e RUN_POSTGRES_TESTS=true:

```sh
./mvnw -B -ntp clean verify
```

Windows: `mvnw.cmd -B -ntp clean verify`. Nunca usar o banco de produção. Backend validation executa a suíte com PostgreSQL descartável.

## Produção

- API HTTPS no Render: https://praclinic-api.onrender.com/api
- Frontend principal na Vercel: https://praclinic.vercel.app
- GitHub Pages permanece como publicação secundária/compatibilidade: https://carloseduardofurlan.github.io/GestaoDeClinicas-Frontend-ProjetoFinal-maisPraTi/
- Deploy pela main, com Docker multi-stage Temurin 26 e PORT dinâmica. Secrets ficam no ambiente Render; CORS permite exatamente `https://praclinic.vercel.app` e `https://carloseduardofurlan.github.io`.
- O plano Free pode apresentar cold start; durabilidade e capacidade do banco dependem do plano contratado.

## Acesso de demonstração

Estas contas são exclusivamente fictícias e foram criadas para avaliação acadêmica do +PraClinic. Não representam usuários ou pacientes reais. As credenciais são intencionalmente públicas e exclusivas desse ambiente acadêmico.

- Aplicação: https://praclinic.vercel.app
- API: https://praclinic-api.onrender.com

| Perfil | Usuário | Senha |
| --- | --- | --- |
| Administrador | admin.demo@praclinic.invalid | AdminDemo!26 |
| Recepção | recepcao.demo@praclinic.invalid | RecepcaoDemo!26 |
| Profissional — Clínica Geral | helena.demo@praclinic.invalid | HelenaDemo!26 |
| Profissional — Cardiologia | rafael.demo@praclinic.invalid | RafaelDemo!26 |
| Profissional — Dermatologia | camila.demo@praclinic.invalid | CamilaDemo!26 |
| Profissional — Pediatria | andre.demo@praclinic.invalid | AndreDemo!26 |

As contas e os dados foram provisionados administrativamente pelas APIs reais. O redeploy usa o PostgreSQL persistente e não executa carga ou recriação automática de dados.

## Arquitetura

[Arquitetura, domínio, regras, permissões e operação](docs/ARQUITETURA.md).
