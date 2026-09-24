package br.com.clinica.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "medical_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_appointment", nullable = false, unique = true)
    private Appointment appointment;
    @Column(name = "symptoms", nullable = false, columnDefinition = "text")
    private String symptoms;
    @Column(name = "diagnosis", nullable = false, columnDefinition = "text")
    private String diagnosis;
    @Column(name = "prescription", nullable = false, columnDefinition = "text")
    private String prescription;
}
