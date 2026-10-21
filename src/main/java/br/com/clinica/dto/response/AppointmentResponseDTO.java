package br.com.clinica.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record AppointmentResponseDTO(
    UUID id,
    UUID patientId,
    String patientName,
    UUID professionalId,
    String professionalName,
    LocalDate date,
    LocalTime time,
    UUID statusId,
    String statusName,
    String notes,
    LocalDateTime created
) {}
