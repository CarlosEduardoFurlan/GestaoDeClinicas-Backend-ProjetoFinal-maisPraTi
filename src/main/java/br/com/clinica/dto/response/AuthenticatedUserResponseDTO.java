package br.com.clinica.dto.response;

import java.util.UUID;

public record AuthenticatedUserResponseDTO(
        UUID userId,
        String name,
        String email,
        String profile,
        UUID professionalId
) {
}
