# Relatório de Atividades do Integrante — Sprint 2

**Integrante:** Murilo  
**Módulo / Funcionalidade:** CRUD e Testes Unitários de Pacientes (`Patient`)[cite: 1]  
**Tecnologias Utilizadas:** Java, Spring Boot, Spring Data JPA, Lombok, JUnit 5 e Mockito  

---

## 1. Escopo e Objetivos
Desenvolvimento completo da camada de serviço, regras de negócio, transferência de dados (DTOs) e testes unitários automatizados para o gerenciamento de pacientes na API backend da clínica[cite: 1].

---

## 2. Tarefas e Entregas Realizadas

### A. Persistência e Mapeamento
* Criação e ajuste da entidade `Patient`[cite: 1].
* Interface `PatientRepository` integrada ao Spring Data JPA para comunicação com o banco de dados.

### B. Camada de Serviço (`PatientService`)
Implementação dos métodos do ciclo de vida completo[cite: 1]:
* `create`: Cadastro de novos pacientes[cite: 1].
* `findAll`: Listagem geral de pacientes cadastrados[cite: 1].
* `findById`: Busca individual por UUID[cite: 1].
* `update`: Atualização de dados cadastrais[cite: 1].
* `delete`: Remoção de registros por ID[cite: 1].

**Regras de Negócio e Validações:**
* Validação de unicidade para **CPF** e **E-mail**[cite: 1].
* Lançamento de exceção `IllegalArgumentException` com mensagens amigáveis em caso de duplicidade ou registro não encontrado.

### C. Transferência de Dados (DTOs)
Estruturação e isolamento do modelo de domínio utilizando DTOs específicos[cite: 1]:
* `PatientCreateRequest`
* `PatientUpdateRequest`
* `PatientResponse`

### D. Testes Unitários (`PatientServiceTest`)
* Desenvolvimento da suíte completa de testes utilizando **JUnit 5** e **Mockito**[cite: 1].
* Testes cobrindo fluxos de sucesso e exceção (criação válida, CPF duplicado, busca por ID existente/inexistente, listagem e exclusão)[cite: 1].
* **Resultado dos Testes:** 100% dos testes executados e validados com êxito via Maven (`BUILD SUCCESS` / 6 testes aprovados, 0 falhas, 0 erros)[cite: 1].

---

## 3. Versionamento e Integração Git/GitHub

* **Branch:** `feature/entity-professional`[cite: 1]
* **Histórico de Commits:** Organização do código, restauração de arquivos de configuração e commit das funcionalidades[cite: 1].
* **Sincronização:** Rebase com a branch remota (`git pull --rebase`) e envio bem-sucedido via `git push`[cite: 1].
* **Pull Request:** Abertura formal do Pull Request no repositório GitHub para revisão por pares e mesclagem na branch principal[cite: 1].