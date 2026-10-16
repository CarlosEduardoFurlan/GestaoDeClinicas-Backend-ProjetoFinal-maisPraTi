# Relatório de Atividades do Integrante — Sprint 2

**Integrante:** Murilo  
**Módulo / Funcionalidade:** CRUD e Testes Unitários de Pacientes (`Patient`)[cite: 1]  
**Tecnologias Utilizadas:** Java, Spring Boot, Spring Data JPA, Lombok, JUnit 5 e Mockito[cite: 1]  

---

## 1. Escopo e Objetivos
Desenvolvimento completo da camada de serviço, regras de negócio, transferência de dados (DTOs) e testes unitários automatizados para o gerenciamento de pacientes na API backend da clínica[cite: 1].

---

## 2. Tarefas e Entregas Realizadas

### A. Persistência e Mapeamento
* Criação e ajuste da entidade `Patient`[cite: 1].
* Interface `PatientRepository` integrada ao Spring Data JPA para comunicação com o banco de dados[cite: 1].

### B. Camada de Serviço (`PatientService`)
Implementação dos métodos do ciclo de vida completo[cite: 1]:
* `create`: Cadastro de novos pacientes[cite: 1].
* `findAll`: Listagem geral de pacientes cadastrados[cite: 1].
* `findById`: Busca individual por UUID[cite: 1].
* `update`: Atualização de dados cadastrais[cite: 1].
* `delete`: Exclusão de registros por ID (Análise e política de integridade formalizada)[cite: 1].

**Regras de Negócio e Validações:**
* Validação de unicidade para **CPF** e **E-mail**[cite: 1].
* Verificação de vínculo com `Address` existente em criação e atualização.
* Lançamento de exceção `ResourceNotFoundException` / `IllegalArgumentException` com mensagens claras em caso de duplicidade ou registro não encontrado[cite: 1].

### C. Transferência de Dados (DTOs)
Estruturação e isolamento do modelo de domínio utilizando DTOs específicos[cite: 1]:
* `PatientCreateRequest`
* `PatientUpdateRequest`
* `PatientResponse`

### D. Testes Unitários (`PatientServiceTest`)
* Desenvolvimento da suíte completa de testes utilizando **JUnit 5** e **Mockito**[cite: 1].
* Testes cobrindo fluxos de sucesso e exceção (criação válida, CPF duplicado, busca por ID existente/inexistente, listagem e exclusão)[cite: 1].
* **Resultado dos Testes:** 100% dos testes executados e validados com êxito via Maven (`BUILD SUCCESS` / 11 testes aprovados, 0 falhas, 0 erros)[cite: 1].

---

## 3. Análise da Política de Exclusão (DELETE)

* **Diagnóstico Técnico:** O método `PatientService.delete(UUID)` realiza atualmente a exclusão física do paciente. Contudo, a tabela de agendamentos (`appointments`) mantém vínculo direto de chave estrangeira (`id_patient -> patients.id`).
* **Decisão e Diretriz de Grupo:** A exclusão física direta foi identificada como um risco de integridade para o histórico de agendamentos. Alinhado com a equipe, o endpoint `DELETE /api/patients/{id}` permanece **bloqueado na segurança** nesta Sprint, preservando os registros e mantendo os contratos estáveis sem alterações de schema não autorizadas.

---

## 4. Versionamento e Integração Git/GitHub

* **Branch Atual:** `feature/sprint2-patients-completion`
* **Histórico de Sincronização:** Rebase e alinhamento com a branch `develop` mais recente do grupo.
* **Entregas para o Integrante 4:** Garantia da estabilidade dos contratos `POST`, `GET` e `PUT`, com a documentação explícita do bloqueio mantido no `DELETE` para alinhamento da `SecurityConfig` e `API.md`.
* **Pull Request:** Abertura formal do Pull Request direcionado para a branch `develop`.