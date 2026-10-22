package br.com.clinica.dto.request;


import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AppointmentRequestDTO(
    @NotNull(message = "O ID do paciente é obrigatório")
    UUID patientId,

    @NotNull(message = "O ID do profissional é obrigatório")
    UUID professionalId,

    @NotNull(message = "A data é obrigatória")
    LocalDate date,

    @NotNull(message = "O horário é obrigatório")
    LocalTime time,

    String notes
) {}
