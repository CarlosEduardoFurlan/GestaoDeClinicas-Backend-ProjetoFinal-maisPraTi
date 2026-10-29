package br.com.clinica.dto.response;

import java.util.UUID;

public record MedicalRecordResponse(
        UUID id,
        UUID appointmentId,
        String notes
) {
}
