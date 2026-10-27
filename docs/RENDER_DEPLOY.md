# Preparação do backend para Render

Esta etapa prepara o repositório. Não cria banco, Web Service, conta ADMIN ou
credenciais no Render, nem promove frontend/backend para main.

Arquitetura futura: **GitHub Pages → API HTTPS no Render → PostgreSQL no Render**.
Baseline funcional: backend develop `e183b6bc490af6b60a11b0fb1604986c1ca40e66`;
frontend develop `b81e6caddc2489eefdd79da42c36313bdbcc055d`, sem alterações.

## Container e porta

Dockerfile multi-stage com imagens oficiais `eclipse-temurin:26-jdk-noble`
(build) e `eclipse-temurin:26-jre-noble` (runtime). Tags confirmadas no
[catálogo oficial](https://github.com/docker-library/official-images/blob/master/library/eclipse-temurin)
em 27/10/2026. As tags podem receber patches; não são um pin por digest.

O build copia `.mvn`, `mvnw`, `pom.xml` e `src`; usa o Maven Wrapper incluído:
`./mvnw -B -ntp -DskipTests clean package`. A suíte completa é um gate separado,
também executado pelo CI. Compilação e empacotamento continuam obrigatórios.

O runtime contém JRE e o JAR explícito `clinica-api-0.0.1-SNAPSHOT.jar`, sem
selecionar `.original` ou JARs por wildcard. Executa como usuário sem privilégio,
com `ENTRYPOINT ["java", "-jar", "app.jar"]`; não há banco ou ADMIN na imagem.
`.dockerignore` exclui Git, IDEs, target, arquivos de ambiente e temporários;
preserva Maven Wrapper, fontes e documentação. Nenhum secret é argumento de build.

`server.port=${PORT:8080}` mantém 8080 sem PORT e usa a porta fornecida pelo
Render em runtime. `EXPOSE 8080` é apenas documentação do default, não substitui
PORT, nem publica uma porta automaticamente. Não há profile Render.

## PostgreSQL — criar somente em etapa posterior autorizada

1. Render Dashboard → **New → PostgreSQL**.
2. Nome sugerido: `praclinic-db`; versão **PostgreSQL 18**.
3. Escolher a mesma região do futuro Web Service.
4. Database e User podem ser gerados pelo Render ou definidos conscientemente.
   Não copiar senha para documentação, código, tickets ou logs.
5. Guardar os dados de conexão internos no mecanismo seguro do ambiente.

Para homologação inicial, Free pode ser usado com dados descartáveis. **Free
Postgres expira após 30 dias e não possui backups**; não é banco permanente de
produção. Para entrega durável, definir plano, persistência e backups adequados.
[Limites oficiais](https://render.com/docs/free).

## Web Service — criar somente em etapa posterior autorizada

1. Render Dashboard → **New → Web Service**.
2. Conectar `CarlosEduardoFurlan/GestaoDeClinicas-Backend-ProjetoFinal-maisPraTi`.
3. Branch inicial de homologação: `develop`.
4. Runtime/Language: **Docker**; Dockerfile na raiz (`Dockerfile`).
5. Nome sugerido: `praclinic-api`; mesma região do PostgreSQL.
6. Configurar as variáveis abaixo no ambiente do serviço antes da inicialização.
7. Manter o health check TCP padrão, sem configurar um caminho HTTP.
8. Auto Deploy pode acompanhar develop durante homologação. Antes da release,
   decidir explicitamente se o serviço passará a acompanhar main.

Não são necessários comandos externos de build/start no painel: o Dockerfile
contém o build e o entrypoint. Não publicar imagem em Docker Hub ou GHCR; o
Render fará o build diretamente do repositório.

## Variáveis de runtime

| Nome exato | Configuração posterior |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://<internal-host>:5432/<database>` |
| `DB_USERNAME` | Usuário fornecido/definido no PostgreSQL Render |
| `DB_PASSWORD` | Senha do banco, somente no ambiente |
| `JWT_SECRET` | Base64 válido de no mínimo 32 bytes aleatórios, somente no ambiente |
| `APP_CORS_ALLOWED_ORIGINS` | `https://carloseduardofurlan.github.io` para a homologação do frontend acordado |
| `PORT` | Fornecida pelo Render; não fixar 10000 no código |

Usar os dados **internos** do banco, na mesma região. A URL `postgresql://...`
fornecida pelo painel não é diretamente o formato JDBC esperado por DB_URL.
Converter host/database e manter usuário/senha nas variáveis próprias. Se o
provedor exigir parâmetros SSL adicionais, seguir sua configuração de conexão.
O projeto não lê `DATABASE_URL` como alias.

CORS recebe origem, não URL de página: sem path do repositório, barra final ou
wildcard. O valor fica no painel Render; não foi incluído no código/configuração
default. A política de CORS, JWT, perfis e ownership permanece inalterada.

Gerar JWT_SECRET por um CSPRNG local confiável, codificar os bytes em Base64 e
transferir diretamente para o campo protegido de ambiente. Não usar exemplos,
secret de testes, argumento de linha de comando, commit ou saída compartilhada.
Nenhum secret de produção foi gerado nesta preparação.

## Inicialização, health check e ADMIN

Banco vazio: Flyway aplica **V1–V15**, incluindo o catálogo oficial de status.
Hibernate usa `ddl-auto=validate`; não alterar para create/update. O usuário do
banco precisa das permissões necessárias às migrations.

Não há Actuator/health público nesta baseline. O TCP padrão confirma que a porta
aceita conexões; não verifica toda a lógica da aplicação. Não configurar
`/api/auth/me` como health check HTTP: sem token ele retorna 401.
[Health checks oficiais](https://render.com/docs/health-checks).

O primeiro ADMIN continua manual, após migrations e antes de disponibilizar o
ambiente. Seguir [BOOTSTRAP_ADMIN.md](BOOTSTRAP_ADMIN.md), gerando BCrypt localmente
e inserindo somente o hash em sessão administrativa protegida do banco. Não há
conta default, cadastro público ou migration de credencial. A senha da API tem
limite de 20 caracteres.

O guia de bootstrap contém referências históricas a V1–V13 e `/api/users`
bloqueado. Na baseline atual, são V1–V15 e `/api/users` é permitido somente ao
ADMIN. Esta observação não muda o provisionamento inicial manual.

## HTTPS e frontend posterior

O domínio será atribuído pelo serviço real: `https://<service>.onrender.com`.
Esse é um formato, não um endereço provisionado. Render termina TLS; não instalar
certificado ou keystore no Spring. A API interna atende na PORT fornecida.

Após homologar o backend HTTPS real, sua URL com `/api` poderá ser usada em
VITE_API_URL do build frontend. Isso não foi configurado nesta etapa.

Free Web Service dorme após 15 minutos sem tráfego; o primeiro acesso sofre
cold start, que pode levar cerca de um minuto. Planejar a apresentação e a
homologação considerando isso; não adicionar ping/keepalive artificial.
[Comportamento Free](https://render.com/docs/free).

## Validação local

Com Java 26 e PostgreSQL 18 **exclusivo para testes**, configurar as variáveis de
conexão e `RUN_POSTGRES_TESTS=true`, então executar:

```sh
./mvnw -B -ntp clean verify
```

No Windows: `mvnw.cmd -B -ntp clean verify`. O Spring não lê `.env` automaticamente.
Nunca apontar essa suíte para o banco de produção.

Se Docker estiver disponível:

```sh
docker build -t praclinic-api:render-test .
```

Executar o container com as variáveis passadas somente em runtime e um
PostgreSQL descartável acessível pela rede do container. Não copiar env files
para a imagem. Para a conexão com o host no Docker Desktop, usar o endereço
apropriado dessa plataforma; `localhost` dentro do container é o próprio container.

Testar PORT alternativa e mapear a mesma porta escolhida; sem PORT, o default
continua 8080. Confirmar startup, Flyway, JPA validate, login, `/api/auth/me` e
`/api/statuses`. Inspecionar imagem/history sem imprimir variáveis sensíveis.

O CI Backend validation permanece como gate obrigatório antes do merge.

### Evidências desta preparação — 27/10/2026

- Maven Wrapper `clean verify` com Java 26 e PostgreSQL 18 exclusivo: **282 testes,
  zero falhas, erros ou skips**; `RUN_POSTGRES_TESTS=true`.
- JAR nativo validado sem PORT (8080) e com PORT=10000, em bancos novos: Flyway
  V1–V15, JPA validate, login de fixture, auth/me, statuses e rejeição anônima 401.
- Docker Desktop existente foi iniciado; build real e `docker build --check`
  passaram. O primeiro build detectou CRLF do wrapper no checkout Windows;
  normalização dentro da etapa de build resolveu sem alterar o wrapper original.
- JRE da imagem: Eclipse Temurin 26.0.2.1+1; execução como usuário `app`, não root.
- Container validado com PostgreSQL 18 em rede descartável, sem porta pública do
  banco. PORT=10000 e default 8080 passaram, incluindo login/auth/me/statuses,
  Flyway e JPA. Credenciais de teste foram passadas somente em runtime.
- Imagem/history inspecionados: sem DB_PASSWORD/JWT_SECRET de runtime e sem
  fontes/Maven na etapa final. Nenhum registry recebeu push.
- Fixtures ADMIN existiram apenas nos bancos locais descartáveis, removidos ao
  terminar; não houve provisionamento de ADMIN ou recurso no Render.

O CI remoto da PR ainda precisa concluir com sucesso antes do merge. A primeira
inicialização no Render continua pendente da etapa posterior de infraestrutura.

## Limites desta preparação

Não criar recursos Render, gerar credenciais de produção, provisionar ADMIN no
ambiente real, alterar frontend/VITE_API_URL ou promover develop para main.
O próximo passo depende de revisão e autorização para criar a infraestrutura.
