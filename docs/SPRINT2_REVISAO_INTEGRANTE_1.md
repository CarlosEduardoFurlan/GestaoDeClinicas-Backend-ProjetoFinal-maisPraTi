# Revisão da entrega do integrante 1

Referência: plano de quatro integrantes e guia atualizado da Sprint 2. Revisão em 20/10/2026.

## Contratos disponíveis

- UserService: create(UserRequestDTO), findAll(), findById(UUID), update(UUID, UserUpdateRequestDTO), delete(UUID).
- SpecialtyService: create(SpecialtyRequestDTO), findAll(), findById(UUID), update(UUID, SpecialtyRequestDTO), delete(UUID).
- ProfessionalService: create(ProfessionalRequestDTO), findAll(), findById(UUID), update(UUID, ProfessionalUpdateRequestDTO), delete(UUID).

UserUpdateRequestDTO recebe name, password e profile. O e-mail de login não faz parte da atualização enquanto sua política está pendente. ProfessionalUpdateRequestDTO recebe name, register, telephone, cellPhone, email de contato, specialtyId e address; preserva a conta User associada. Não recebe senha nem perfil.

Somente ADMIN, RECEPTIONIST e PROFESSIONAL, com grafia exata, são aceitos nos services de criação de User e Professional e atualização de User. Senhas passam pelo PasswordEncoder BCrypt existente e não são retornadas.

ResourceNotFoundException corresponde a 404; BusinessException a 400. Conflitos de FK/unicidade do banco permanecem DataIntegrityViolationException, mapeada para 409 pelo handler existente. Exclusão de Professional não exclui User, Address, Schedule ou Appointment. Exclusão de Specialty respeita FK; não existe conceito de profissional ativo/inativo.

## Limites e decisões pendentes

Nenhuma rota ou permissão foi acrescentada nesta entrega. As futuras rotas administrativas continuam sujeitas ao denyAll até aprovação. Alteração do e-mail de autenticação permanece fora do contrato. UserService.delete respeita o schema histórico, que contém ON DELETE CASCADE de users para professionals; essa operação não deve ser liberada por HTTP sem revisão explícita da política de exclusão de usuários. Não foi criada cascade nem alterada migration.

## Evidências

A versão original da PR #12 falhou com 1 falha e 1 erro: testes usavam USER como perfil válido. O contrato oficial exige rejeitá-lo. A revisão usa perfis aprovados nos cenários válidos e testa explicitamente a rejeição de USER e de outros valores inválidos.

Testes adicionais verificam BCrypt real, identidade de login preservada, conta preservada ao editar/excluir Professional, specialty inexistente, registro duplicado e FKs em PostgreSQL exclusivo. O ID de User passa a ser gerado pelo mapeamento JPA existente, sem atribuição manual conflitante com @GeneratedValue.
