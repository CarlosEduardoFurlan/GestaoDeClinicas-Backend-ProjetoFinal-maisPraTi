package br.com.clinica.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SpecialtyResponseDTO(
        UUID id,
        String name,
        String description,
        OffsetDateTime created
) {}
