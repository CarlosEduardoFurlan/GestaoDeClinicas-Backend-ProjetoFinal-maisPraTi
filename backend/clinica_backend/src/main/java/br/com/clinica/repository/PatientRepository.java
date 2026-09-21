package br.com.clinica.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.clinica.entity.Patient;

public interface PatientRepository extends JpaRepository<Patient, UUID> {
    boolean existsByCpf(String cpf);
    boolean existsByEmail(String email);
}