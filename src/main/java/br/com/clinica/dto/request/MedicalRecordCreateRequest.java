package br.com.clinica.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MedicalRecordCreateRequest(
        @NotNull UUID appointmentId,
        @NotNull String notes
) {
}
