package br.com.clinica.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "address")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "address", length = 150, nullable = false)
    private String address;

    @Column(name = "number", nullable = false)
    private Integer number;

    @Column(name = "complement", length = 20)
    private String complement;

    @Column(name = "district", length = 50, nullable = false)
    private String district;

    @Column(name = "city", length = 100, nullable = false)
    private String city;

    @Column(name = "uf", length = 2, nullable = false)
    private String uf;

    @Column(name = "zip_code", length = 9, nullable = false)
    private String zipCode;

    @Column(name = "created", insertable = false, updatable = false)
    private LocalDateTime created;
}