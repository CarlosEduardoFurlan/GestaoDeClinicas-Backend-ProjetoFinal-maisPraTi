package br.com.clinica.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SpecialtyRequestDTO(
        @NotBlank(message = "O nome da especialidade é obrigatório.")
        @Size(max = 50, message = "O nome da especialidade deve ter no máximo 50 caracteres.")
        String name,
        String description
) {}
