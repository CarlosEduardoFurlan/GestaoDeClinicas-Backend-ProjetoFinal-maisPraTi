package br.com.clinica.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ProfessionalRequestDTO(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 30) String register,
    @NotBlank @Size(max = 20) String telephone,
    @NotBlank @Size(max = 20) String cellPhone,
    @NotBlank @Email @Size(max = 100) String email,
    @NotNull UUID specialtyId,
    @NotNull @Valid AddressRequestDTO address,
    @NotNull @Valid UserRequestDTO user
) {}