package br.com.clinica.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "professionals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Professional {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    @Column(name = "register", nullable = false, unique = true, length = 30)
    private String register;
    @Column(name = "telephone", nullable = false, length = 20)
    private String telephone;
    @Column(name = "cell_phone", nullable = false, length = 20)
    private String cellPhone;
    @Column(name = "email", nullable = false, length = 100)
    private String email;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_speciality", nullable = false)
    private Specialty specialty;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_address", nullable = false)
    private Address address;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user", nullable = false, unique = true)
    private User user;
}
