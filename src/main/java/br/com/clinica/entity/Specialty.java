package br.com.clinica.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "specialties")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

public class Specialty {
    @Id
    @Generated
    private UUID id;

    private String name;
    private String description;
    private ZonedDateTime created;
}
