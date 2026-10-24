package br.com.clinica.service;

import br.com.clinica.dto.request.ScheduleRequestDTO;
import br.com.clinica.entity.*;
import br.com.clinica.exception.*;
import br.com.clinica.repository.*;
import br.com.clinica.security.CurrentUserAccess;
import br.com.clinica.security.Role;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScheduleServiceTest {
    private final ScheduleRepository schedules = mock(ScheduleRepository.class);
    private final ProfessionalRepository professionals = mock(ProfessionalRepository.class);
    private final CurrentUserAccess access = mock(CurrentUserAccess.class);
    private final ScheduleService service = new ScheduleService(schedules, professionals, access);
    private final ScheduleRequestDTO request = new ScheduleRequestDTO(UUID.randomUUID(), LocalDate.of(2000, 1, 1), LocalTime.NOON);

    @org.junit.jupiter.api.BeforeEach void actor() { when(access.role()).thenReturn(Role.ADMIN); }
    @Test void createsSlotWithoutInventingTemporalRestriction() {
        var professional = new Professional(); professional.setId(request.professionalId()); professional.setName("Ana");
        when(professionals.findById(request.professionalId())).thenReturn(Optional.of(professional));
        when(schedules.save(any())).thenAnswer(call -> call.getArgument(0));
        var response = service.create(request);
        assertEquals(request.date(), response.date());
        assertEquals(request.professionalId(), response.professionalId());
    }
    @Test void rejectsMissingProfessional() {
        assertThrows(ResourceNotFoundException.class, () -> service.create(request));
        verifyNoInteractions(schedules);
    }
    @Test void rejectsDuplicateSlot() {
        when(professionals.findById(request.professionalId())).thenReturn(Optional.of(new Professional()));
        when(schedules.existsByProfessional_IdAndDateAndTime(request.professionalId(), request.date(), request.time())).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.create(request));
        verify(schedules, never()).save(any());
    }
    @Test void listsSlotsWithProfessionalIdentityAndOffsetTimestamp() {
        var professional = new Professional(); professional.setId(request.professionalId()); professional.setName("Ana");
        var created = OffsetDateTime.now();
        var slot = new Schedule(UUID.randomUUID(), professional, request.date(), request.time(), created);
        when(schedules.findAll()).thenReturn(List.of(slot));
        var response = service.findAll().getFirst();
        assertEquals(created, response.created()); assertEquals("Ana", response.professionalName());
    }
}
