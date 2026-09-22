package br.com.clinica.dto;

import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientResponse {

    private UUID id;
    private String name;
    private String cpf;
    private String email;
    private String phone;
    private LocalDate birthDate;
}