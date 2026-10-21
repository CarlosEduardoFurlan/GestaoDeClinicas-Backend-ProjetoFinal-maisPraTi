package br.com.clinica.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// E-mail de autenticação não integra a atualização enquanto a política está pendente.
public record UserUpdateRequestDTO(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 20) String password,
        @NotBlank @Pattern(regexp = "ADMIN|RECEPTIONIST|PROFESSIONAL") String profile
) {}
