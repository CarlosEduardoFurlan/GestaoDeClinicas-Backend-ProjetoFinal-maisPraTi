package br.com.clinica.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record PatientResponse(
        UUID id,
        String name,
        String cpf,
        LocalDate dateBirth,
        String telephone,
        String cellPhone,
        String email,
        String gender,
        UUID addressId
) {
}
