package br.com.clinica.dto.response;

import java.util.UUID;

public record ProfessionalResponseDTO(
    UUID id,
    String name,
    String register,
    String telephone,
    String cellPhone,
    String email,
    String specialtyName,
    String cityName,
    UUID userId
) {}
