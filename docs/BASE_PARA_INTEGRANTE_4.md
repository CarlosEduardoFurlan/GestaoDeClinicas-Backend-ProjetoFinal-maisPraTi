# Base para o integrante 4

Referência: Plano de Sprints do Back-end (4 integrantes), guias individuais e padrões de domínio/API do projeto. A divisão de 4 integrantes prevalece sobre a divisão genérica anterior de 8 integrantes.

## Entregas revisadas nesta integração

- PR #5: exceções e validação, com respostas 400/404 e correções para JSON inválido, UUID inválido, método HTTP inválido e conflitos de integridade.
- PR #7: contratos e CRUD de pacientes adaptados ao schema oficial; testes recuperados e ampliados; verificação de CPF/e-mail duplicados.
- Código de profissionais recuperado da pasta antiga: criação e consulta, DTOs e repositories. Senhas novas são gravadas com BCrypt; respostas não incluem senha.
- Nove entidades e nove repositories na aplicação da raiz, sem backend duplicado. Colunas PostgreSQL `text` não usam `@Lob`/OID.
- V1–V13 preservadas e executadas em PostgreSQL 18 vazio, com Hibernate `ddl-auto=validate`.
- Credenciais de banco por variáveis de ambiente e Maven Wrapper para execução reproduzível.

## O que você pode iniciar

Sprint 1, branch sugerida `feature/api-foundation`, criada da `develop` atualizada: revisar os controllers existentes, organizar o padrão HTTP e documentar os contratos. Não recriar as regras de pacientes/profissionais nos controllers.

| Recurso | Contrato disponível |
| --- | --- |
| Pacientes | POST/GET `/api/patients`, GET/PUT/DELETE `/api/patients/{id}` |
| Profissionais | POST/GET `/api/professionals`, GET `/api/professionals/{id}` |
| Endereço do paciente | `addressId` de endereço já persistido; endpoint próprio ainda não implementado |
| Cadastro de profissional | `ProfessionalRequestDTO`, com endereço e usuário aninhados e `specialtyId` existente |
| Erro | `ApiErrorResponse`: timestamp, status, error, message, path e details |

Sprint 2: reutilizar o bean `PasswordEncoder` de `PasswordConfig` ao implementar login/Spring Security/JWT; não criar um segundo bean concorrente. A dependência atual é apenas `spring-security-crypto`, sem autenticação ou bloqueio de rotas. Perfis e permissões precisam ser definidos na sua implementação; o campo de perfil recebido no cadastro de profissional exige proteção administrativa antes da exposição pública.

## Pendências reais da Sprint 2 dos demais integrantes

- Integrante 1: UserService, SpecialtyService e atualização/exclusão de profissionais não constavam nas PRs entregues.
- Integrante 3: ScheduleService e AppointmentService (disponibilidade, reserva, status, cancelamento e testes) não constavam na PR #5.
- O banco garante unicidade do slot em `schedule`, mas ainda não impede duas linhas em `appointments` no mesmo horário. Essa regra precisa ser resolvida junto com reserva/cancelamento e nova migration, sem alterar as migrations antigas.
- Não há schema aprovado para Measurement; não criar entidade/tabela por suposição.

Essas pendências não impedem iniciar sua Sprint 1 ou preparar segurança, mas impedem declarar toda a Sprint 2 concluída. Controllers desses módulos devem aguardar services/DTOs reais.

## Validação local

Use JDK 26. `./mvnw test` (Linux) ou `./mvnw.cmd test` (Windows) executa os testes unitários. Os testes PostgreSQL são opt-in para não tocar em banco de desenvolvimento inadvertidamente.

Crie um banco PostgreSQL vazio e exclusivo de testes. Configure `RUN_POSTGRES_TESTS=true`, `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` apontando para ele e execute `./mvnw verify`. O contexto executa Flyway e valida as entidades. Os testes de API rodam com rollback. O workflow do GitHub faz essa validação em PostgreSQL descartável.

O arquivo `.env.example` é referência; Spring Boot não carrega `.env` automaticamente. Configure as variáveis no terminal ou na IDE. Senhas antigas gravadas em texto puro precisam ser redefinidas antes de usar autenticação; a integração não modifica dados de bancos existentes.
