package br.com.clinica.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.clinica.entity.Professional;

@Repository
public interface ProfessionalRepository extends JpaRepository<Professional, UUID> {
boolean existsByRegister(String register);
}
