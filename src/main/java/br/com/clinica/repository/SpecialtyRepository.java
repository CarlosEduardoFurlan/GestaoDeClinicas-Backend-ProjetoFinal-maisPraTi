package br.com.clinica.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.clinica.entity.Specialty;

@Repository
public interface SpecialtyRepository extends JpaRepository<Specialty, UUID> {
}
