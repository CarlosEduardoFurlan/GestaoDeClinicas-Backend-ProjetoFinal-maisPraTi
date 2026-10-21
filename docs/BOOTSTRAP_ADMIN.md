# Primeiro administrador

O sistema é interno. Não existe cadastro público nem /api/auth/register. O primeiro ADMIN é provisionado manualmente por quem administra o ambiente, após aplicar Flyway e antes de disponibilizar a API.

1. Configure PostgreSQL e as variáveis de conexão do ambiente. Execute a aplicação com JWT_SECRET externa para aplicar as migrations V1–V13; confirme a validação JPA. Não crie tabelas à mão.
2. Escolha nome, e-mail exclusivo e senha do administrador. O contrato atual de senha da API tem limite de 20 caracteres. Não use os exemplos da documentação como credenciais reais.
3. Gere localmente um hash com BCryptPasswordEncoder do Spring Security, a mesma implementação usada pelo backend. Use uma ferramenta local confiável com entrada protegida; não envie a senha a um gerador online e não a coloque em argumentos de linha de comando ou histórico do terminal. Confira localmente que matches(senha, hash) retorna true. O resultado deve ser um hash BCrypt compatível com users.password varchar(60).
4. Em uma sessão administrativa protegida do banco, insira uma linha em users com name, email, password contendo somente o hash e profile igual a ADMIN. Omita id e created para usar os defaults existentes. Prefira parâmetros na ferramenta de banco; não grave a instrução preenchida em arquivos do repositório nem em tickets/logs compartilhados.
5. Confirme login em POST /api/auth/login e acesso a uma rota aprovada. Confirme que senha incorreta retorna 401. Guarde a senha apenas no mecanismo seguro escolhido para o ambiente.
6. Remova arquivos temporários que contenham segredo/hash real e verifique que nada disso foi adicionado ao Git. JWT_SECRET é um segredo independente da senha do administrador: use pelo menos 32 bytes aleatórios representados em Base64.

Não há conta ou senha padrão. Não existe migration de credencial nem seed de usuário real. O endpoint administrativo /api/users está preparado, mas permanece bloqueado até aprovação explícita da matriz de acesso. A criação inicial manual não autoriza liberar esse endpoint nem alterar roles.

