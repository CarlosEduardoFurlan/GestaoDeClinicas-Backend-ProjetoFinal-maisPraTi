package br.com.clinica.repository;

import br.com.clinica.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalTime;

public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {
    boolean existsByProfessional_IdAndDateAndTime(UUID professionalId, LocalDate date, LocalTime time);
}
