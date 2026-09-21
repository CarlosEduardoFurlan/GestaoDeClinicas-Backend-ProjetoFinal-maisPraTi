package br.com.clinica.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "address")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter


public class Address {
    @Id
    @Generated
    private UUID id;

    private String address;
    private int number;
    private String complement;
    private String district;
    private String city;
    private String uf;
    private String zip_code;
    private ZonedDateTime created;
}
