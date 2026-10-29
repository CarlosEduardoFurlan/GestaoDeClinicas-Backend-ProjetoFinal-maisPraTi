package br.com.clinica.dto.response;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponseDTO(
    UUID id,
    String name,
    String email,
    String profile,
    OffsetDateTime created
) {}
