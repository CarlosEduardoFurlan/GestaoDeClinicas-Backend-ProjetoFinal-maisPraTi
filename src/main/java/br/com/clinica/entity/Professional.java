package br.com.clinica.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "professionals")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

public class Professional {
    @Id
    @Generated
    private UUID id;

    private String name;
    private String register;
    private String telephone;
    private String cell_phone;
    private String email;
    private UUID id_speciality;
    private UUID id_address;
    private UUID id_user;
}
