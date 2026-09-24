package br.com.clinica.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record PatientUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Past LocalDate dateBirth,
        @Size(max = 20) String telephone,
        @NotBlank @Size(max = 20) String cellPhone,
        @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 1, max = 1) String gender,
        @NotNull UUID addressId
) {
}
