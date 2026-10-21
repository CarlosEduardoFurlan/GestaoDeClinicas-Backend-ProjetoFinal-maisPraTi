package br.com.clinica.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduleResponseDTO(
    UUID id,
    UUID professionalId,
    String professionalName,
    LocalDate date,
    LocalTime time,
    OffsetDateTime created
) {}
