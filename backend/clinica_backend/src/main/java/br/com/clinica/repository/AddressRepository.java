package br.com.clinica.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.clinica.entity.Address;

public interface AddressRepository extends JpaRepository<Address, UUID> {
}