package br.com.clinica.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "status")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

public class Status {
    @Id
    @Generated
    private UUID id;

    private String description;
}
