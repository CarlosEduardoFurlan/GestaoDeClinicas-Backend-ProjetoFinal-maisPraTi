package br.com.clinica.repository;

import br.com.clinica.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalTime;

public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {
    boolean existsByProfessional_IdAndDateAndTime(UUID professionalId, LocalDate date, LocalTime time);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select schedule from Schedule schedule
            where schedule.professional.id = :professionalId
              and schedule.date = :date
              and schedule.time = :time
            """)
    Optional<Schedule> findSlotForUpdate(@Param("professionalId") UUID professionalId,
                                         @Param("date") LocalDate date,
                                         @Param("time") LocalTime time);
}
