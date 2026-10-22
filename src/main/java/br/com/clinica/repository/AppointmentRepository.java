package br.com.clinica.repository;

import br.com.clinica.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    boolean existsByProfessional_IdAndDateAndTimeAndStatus_DescriptionNot(
            UUID professionalId, LocalDate date, LocalTime time, String statusDescription);
}
