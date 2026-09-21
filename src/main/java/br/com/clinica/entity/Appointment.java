package br.com.clinica.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointments")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

public class Appointment {
    @Id
    @Generated
    private UUID id;

    private UUID id_patient;
    private UUID id_professional;
    private ZonedDateTime date;
    private ZonedDateTime time;
    private UUID id_status;
    private ZonedDateTime created;

    @Lob
    private String notes;
}
