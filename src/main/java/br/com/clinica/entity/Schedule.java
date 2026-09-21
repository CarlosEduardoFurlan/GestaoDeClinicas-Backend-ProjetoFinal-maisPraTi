package br.com.clinica.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "schedule")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

public class Schedule {
    @Id
    @Generated
    private UUID id;

    private UUID id_professional;
    private ZonedDateTime date;
    private ZonedDateTime time;
    private ZonedDateTime created;
}
