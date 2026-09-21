package br.com.clinica.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "patients")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

public class Patient {
    @Id
    @Generated
    private UUID id;

    private String name;
    private String cpf;
    private ZonedDateTime date_birth;
    private String telephone;
    private String cell_phone;
    private String email;
    private ZonedDateTime created;
    private char gender;
    private UUID id_address;
}
