package br.com.clinica.repository;

import br.com.clinica.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, UUID> {
}
