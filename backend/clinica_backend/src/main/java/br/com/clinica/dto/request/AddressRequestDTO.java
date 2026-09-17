package br.com.clinica.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddressRequestDTO(
    @NotBlank @Size(max = 150) String address,
    @NotNull Integer number,
    @Size(max = 20) String complement,
    @NotBlank @Size(max = 50) String district,
    @NotBlank @Size(max = 100) String city,
    @NotBlank @Size(max = 2) String uf,
    @NotBlank @Size(max = 9) String zipCode
) {}