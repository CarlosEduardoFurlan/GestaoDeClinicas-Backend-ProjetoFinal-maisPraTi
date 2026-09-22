package br.com.clinica.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequestDTO(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Email @Size(max = 100) String email,
    @NotBlank @Size(max = 20) String password,
    @NotBlank @Size(max = 25) String profile
) {}