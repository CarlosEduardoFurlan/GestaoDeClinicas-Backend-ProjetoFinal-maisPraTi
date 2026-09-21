package br.com.clinica.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter


public class User {

    @Id
    @Generated
    private UUID id;

    private String name;
    private String email;
    private String password;
    private String profile;
    private ZonedDateTime timestamptz;

}
