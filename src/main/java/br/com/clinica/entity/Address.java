package br.com.clinica.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "address")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "address", nullable = false, length = 150)
    private String address;
    @Column(name = "number", nullable = false)
    private Integer number;
    @Column(name = "complement", length = 20)
    private String complement;
    @Column(name = "district", nullable = false, length = 50)
    private String district;
    @Column(name = "city", nullable = false, length = 100)
    private String city;
    @Column(name = "uf", nullable = false, length = 2)
    private String uf;
    @Column(name = "zip_code", nullable = false, length = 9)
    private String zipCode;
    @Column(name = "created", insertable = false, updatable = false)
    private OffsetDateTime created;
}
