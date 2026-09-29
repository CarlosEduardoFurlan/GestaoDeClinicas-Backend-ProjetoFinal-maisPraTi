package br.com.clinica.dto.response;

public record LoginResponseDTO(
        String accessToken,
        String tokenType,
        long expiresIn
) {}