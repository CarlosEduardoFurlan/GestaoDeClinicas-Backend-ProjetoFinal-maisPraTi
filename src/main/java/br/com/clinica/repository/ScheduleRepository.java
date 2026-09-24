package br.com.clinica.repository;

import br.com.clinica.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {
}
