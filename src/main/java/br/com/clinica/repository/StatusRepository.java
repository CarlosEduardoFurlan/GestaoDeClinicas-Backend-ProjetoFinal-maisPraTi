package br.com.clinica.repository;

import br.com.clinica.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface StatusRepository extends JpaRepository<Status, UUID> {
}
