package br.com.clinica.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.clinica.entity.Professional;

public interface ProfessionalRepository extends JpaRepository<Professional, UUID> {

    // Método customizado para buscar profissional por registro (CRM/CRO/etc.)
    Optional<Professional> findByRegister(String register);

    // Método customizado para buscar profissional pelo e-mail
    Optional<Professional> findByEmail(String email);

    // Método para verificar se já existe um cadastro com o mesmo registro
    boolean existsByRegister(String register);
}