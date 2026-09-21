package br.com.clinica.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.clinica.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
boolean existsByEmail(String email);
}