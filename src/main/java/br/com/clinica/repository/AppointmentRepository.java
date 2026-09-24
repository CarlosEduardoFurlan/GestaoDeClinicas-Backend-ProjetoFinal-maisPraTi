package br.com.clinica.repository;

import br.com.clinica.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
}
