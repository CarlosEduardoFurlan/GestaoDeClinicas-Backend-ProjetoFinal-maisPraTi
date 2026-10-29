package br.com.clinica.dto.request;

import jakarta.validation.constraints.NotNull;

public record MedicalRecordUpdateRequest(
        @NotNull String notes
) {
}
