package br.com.clinica.service;

import br.com.clinica.dto.request.AppointmentRequestDTO;
import br.com.clinica.dto.request.AppointmentStatusUpdateRequestDTO;
import br.com.clinica.entity.Appointment;
import br.com.clinica.entity.Patient;
import br.com.clinica.entity.Professional;
import br.com.clinica.entity.Schedule;
import br.com.clinica.entity.Status;
import br.com.clinica.exception.BusinessException;
import br.com.clinica.exception.ConflictException;
import br.com.clinica.repository.AppointmentRepository;
import br.com.clinica.repository.PatientRepository;
import br.com.clinica.repository.ProfessionalRepository;
import br.com.clinica.repository.ScheduleRepository;
import br.com.clinica.repository.StatusRepository;
import br.com.clinica.security.CurrentUserAccess;
import br.com.clinica.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentServiceTest {
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final PatientRepository patients = mock(PatientRepository.class);
    private final ProfessionalRepository professionals = mock(ProfessionalRepository.class);
    private final ScheduleRepository schedules = mock(ScheduleRepository.class);
    private final StatusRepository statuses = mock(StatusRepository.class);
    private final CurrentUserAccess access = mock(CurrentUserAccess.class);
    private final AppointmentService service = new AppointmentService(
            appointments, patients, professionals, schedules, statuses, access);

    private final Patient patient = new Patient();
    private final Professional professional = new Professional();
    private final LocalDate date = LocalDate.of(2026, 11, 10);
    private final LocalTime time = LocalTime.of(9, 30);

    @BeforeEach
    void entities() {
        when(access.role()).thenReturn(Role.ADMIN);
        patient.setId(UUID.randomUUID());
        patient.setName("Maria");
        professional.setId(UUID.randomUUID());
        professional.setName("Ana");
    }

    @Test
    void createsAppointmentWithScheduledStatusAfterLockingExistingSlot() {
        var request = new AppointmentRequestDTO(patient.getId(), professional.getId(), date, time, "Retorno");
        var scheduled = status(AppointmentService.SCHEDULED);
        when(patients.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(professionals.findById(professional.getId())).thenReturn(Optional.of(professional));
        when(schedules.findSlotForUpdate(professional.getId(), date, time)).thenReturn(Optional.of(new Schedule()));
        when(statuses.findByDescription(AppointmentService.SCHEDULED)).thenReturn(Optional.of(scheduled));
        when(appointments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(request);

        assertEquals(AppointmentService.SCHEDULED, response.statusName());
        assertEquals("Retorno", response.notes());
        verify(schedules).findSlotForUpdate(professional.getId(), date, time);
    }

    @Test
    void rejectsAppointmentWhenActiveReservationAlreadyOccupiesLockedSlot() {
        var request = new AppointmentRequestDTO(patient.getId(), professional.getId(), date, time, null);
        when(patients.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(professionals.findById(professional.getId())).thenReturn(Optional.of(professional));
        when(schedules.findSlotForUpdate(professional.getId(), date, time)).thenReturn(Optional.of(new Schedule()));
        when(appointments.existsByProfessional_IdAndDateAndTimeAndStatus_DescriptionNot(
                professional.getId(), date, time, AppointmentService.CANCELLED)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(request));
        verify(appointments, never()).save(any());
    }

    @ParameterizedTest
    @MethodSource("validTransitions")
    void acceptsEveryApprovedStatusTransition(String current, String target) {
        var appointment = appointment(current);
        var targetStatus = status(target);
        when(appointments.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(statuses.findById(targetStatus.getId())).thenReturn(Optional.of(targetStatus));
        when(appointments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateStatus(appointment.getId(),
                new AppointmentStatusUpdateRequestDTO(targetStatus.getId()));

        assertEquals(target, response.statusName());
    }

    @ParameterizedTest
    @MethodSource("invalidTransitions")
    void rejectsEveryUnapprovedStatusTransition(String current, String target) {
        var appointment = appointment(current);
        var targetStatus = status(target);
        when(appointments.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(statuses.findById(targetStatus.getId())).thenReturn(Optional.of(targetStatus));

        assertThrows(BusinessException.class, () -> service.updateStatus(appointment.getId(),
                new AppointmentStatusUpdateRequestDTO(targetStatus.getId())));
        verify(appointments, never()).save(any());
    }

    @Test
    void cancellationChangesStatusAndNeverDeletesAppointment() {
        var appointment = appointment(AppointmentService.SCHEDULED);
        var cancelled = status(AppointmentService.CANCELLED);
        when(appointments.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(statuses.findByDescription(AppointmentService.CANCELLED)).thenReturn(Optional.of(cancelled));

        service.cancel(appointment.getId());

        assertEquals(AppointmentService.CANCELLED, appointment.getStatus().getDescription());
        verify(appointments).save(appointment);
        verify(appointments, never()).delete(any());
    }

    @Test
    void listsAppointmentsWithRealRelationshipIdentifiers() {
        var appointment = appointment(AppointmentService.SCHEDULED);
        appointment.setNotes("x".repeat(501));
        when(appointments.findAll()).thenReturn(List.of(appointment));

        var result = service.findAll().getFirst();

        assertEquals(patient.getId(), result.patientId());
        assertEquals(professional.getId(), result.professionalId());
        assertEquals(501, result.notes().length());
    }

    private Appointment appointment(String description) {
        var appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setPatient(patient);
        appointment.setProfessional(professional);
        appointment.setDate(date);
        appointment.setTime(time);
        appointment.setStatus(status(description));
        return appointment;
    }

    private static Status status(String description) {
        return new Status(UUID.randomUUID(), description);
    }

    private static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of("Agendada", "Confirmada"),
                Arguments.of("Agendada", "Cancelada"),
                Arguments.of("Confirmada", "Em atendimento"),
                Arguments.of("Confirmada", "Cancelada"),
                Arguments.of("Em atendimento", "Concluída"));
    }

    private static Stream<Arguments> invalidTransitions() {
        var statuses = List.of("Agendada", "Confirmada", "Em atendimento", "Concluída", "Cancelada");
        var valid = List.of("Agendada->Confirmada", "Agendada->Cancelada",
                "Confirmada->Em atendimento", "Confirmada->Cancelada", "Em atendimento->Concluída");
        return statuses.stream().flatMap(current -> statuses.stream()
                .filter(target -> !valid.contains(current + "->" + target))
                .map(target -> Arguments.of(current, target)));
    }
}
