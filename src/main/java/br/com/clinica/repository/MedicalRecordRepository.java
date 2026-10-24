package br.com.clinica.repository;

import br.com.clinica.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, UUID> {
    boolean existsByAppointment_Id(UUID appointmentId);
    Optional<MedicalRecord> findByAppointment_Id(UUID appointmentId);
    List<MedicalRecord> findAllByAppointment_Professional_Id(UUID professionalId);
}
