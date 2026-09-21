package br.com.clinica.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "medical_records")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

public class MedicalRecord {
    @Id
    @Generated
    private UUID id;

    private UUID id_appointment;

    @Lob
    private String symptoms;

    @Lob
    private String diagnosis;

    @Lob
    private String prescription;
}
