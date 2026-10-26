# DELETE User com dependências — correção de integridade

Data: 26/10/2026. Base: develop `5e2f9422426a8b6adcfa69d1254ddcf29a55c19f`.
Branch: `fix/user-delete-integrity-conflict`.

## Reprodução e causa raiz

`UserDeletePostgresTest` usa Spring Boot, MockMvc com login/JWT reais e
PostgreSQL real. Não há @Transactional no teste: as fixtures são confirmadas
antes do DELETE, que possui sua própria transação de serviço.

Na baseline, o teste de User → Professional → Schedule esperava 409 e recebeu
500. A exceção resolvida foi `InvalidDataAccessApiUsageException`, com causas
`IllegalStateException` e `org.hibernate.TransientPropertyValueException`.
O Professional gerenciado ainda referencia o User marcado como removido.
A falha do grafo acontece no flush, antes de o PostgreSQL avaliar o DELETE.
Logo, apenas antecipar flush não corrige a origem; não era simplesmente uma
DataIntegrityViolationException tardia no commit.

## Correção mínima

UserRepository expõe um DELETE JPQL modificador por UUID, executado dentro da
transação existente do UserService. A query não carrega o grafo inverso User /
Professional. O PostgreSQL aplica a cascata e as constraints já existentes.
Zero linhas afetadas gera a ResourceNotFoundException existente (404).
Uma FK impeditiva gera SQLState 23503, traduzida em DataIntegrityViolationException
pelo repository e tratada pelo GlobalExceptionHandler existente como 409.

Não há captura genérica, mudança de handler, migrations, FKs, ON DELETE,
soft delete, active ou exclusão automática de Schedule/Appointment.
A cascata de User para Professional sem impedimento continua sendo a original
do banco. Clear automático evita referências gerenciadas obsoletas após o bulk
DELETE; flush automático preserva a ordem das alterações pendentes na transação.

## Testes e execução

Quatro cenários PostgreSQL novos:

1. User com Professional/Schedule: 409, mantendo os três registros e suas FKs.
2. User livre: 204 e ausência persistida.
3. UUID inexistente: 404.
4. User/Professional sem vínculo impeditivo: 204; cascata original remove
   Professional e preserva Address.

Para executar, configurar DB_URL, DB_USERNAME e DB_PASSWORD de um banco
exclusivo descartável, habilitar RUN_POSTGRES_TESTS=true e usar Java 26:

```text
mvnw.cmd -B -Dtest=UserDeletePostgresTest test
mvnw.cmd -B clean verify
```

Validação local: PostgreSQL 18 exclusivo em 127.0.0.1:55432, SCRAM, banco
homologacao, Flyway V1–V15 e JPA ddl-auto=validate. Clean verify: **275 testes,
zero falhas/erros/ignorados**; JAR Spring Boot empacotado com sucesso.
JAR executado em Java 26, login ADMIN real e smoke HTTP do DELETE 409/204/404,
conferindo preservação de User/Professional/Schedule no conflito.
Credenciais aleatórias apenas no ambiente temporário, sem secrets neste documento.

O workflow Backend validation já executa verify com PostgreSQL 18 e
RUN_POSTGRES_TESTS=true nas PRs para develop. O novo teste participa desse gate.

Arquivos funcionais alterados: UserRepository.java e UserService.java.
Teste: UserDeletePostgresTest.java. Relatório: este arquivo.
A revalidação específica do frontend ocorre após o merge backend, sem ajuste
de aplicação frontend para mascarar status HTTP.
