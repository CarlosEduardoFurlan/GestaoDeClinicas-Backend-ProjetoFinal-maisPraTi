package br.com.clinica.service;

import br.com.clinica.dto.request.AppointmentStatusUpdateRequestDTO;
import br.com.clinica.dto.request.MedicalRecordCreateRequest;
import br.com.clinica.dto.request.MedicalRecordUpdateRequest;
import br.com.clinica.dto.request.ScheduleRequestDTO;
import br.com.clinica.entity.*;
import br.com.clinica.repository.*;
import br.com.clinica.security.CurrentUserAccess;
import br.com.clinica.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OwnershipPolicyServiceTest {
    private final ScheduleRepository schedules = mock(ScheduleRepository.class);
    private final ProfessionalRepository professionals = mock(ProfessionalRepository.class);
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final PatientRepository patients = mock(PatientRepository.class);
    private final StatusRepository statuses = mock(StatusRepository.class);
    private final MedicalRecordRepository records = mock(MedicalRecordRepository.class);
    private final CurrentUserAccess access = mock(CurrentUserAccess.class);
    private final ScheduleService scheduleService = new ScheduleService(schedules, professionals, access);
    private final AppointmentService appointmentService = new AppointmentService(
            appointments, patients, professionals, schedules, statuses, access);
    private final MedicalRecordService recordService = new MedicalRecordService(records, appointments, access);

    @Test void professionalListsOnlyOwnSchedulesAndCanCreateOnlyOwn() {
        var own = professional();
        var slot = new Schedule(UUID.randomUUID(), own, LocalDate.now(), LocalTime.NOON, null);
        when(access.role()).thenReturn(Role.PROFESSIONAL);
        when(access.professionalId()).thenReturn(own.getId());
        when(schedules.findAllByProfessional_Id(own.getId())).thenReturn(List.of(slot));
        assertEquals(own.getId(), scheduleService.findAll().getFirst().professionalId());
        verify(schedules, never()).findAll();

        var request = new ScheduleRequestDTO(own.getId(), LocalDate.now(), LocalTime.NOON);
        when(professionals.findById(own.getId())).thenReturn(Optional.of(own));
        when(schedules.save(any())).thenAnswer(call -> call.getArgument(0));
        scheduleService.create(request);
        verify(access).requireOwnProfessional(own.getId());
    }

    @Test void professionalCannotCreateScheduleForAnotherProfessional() {
        when(access.role()).thenReturn(Role.PROFESSIONAL);
        var otherId = UUID.randomUUID();
        doThrow(new AccessDeniedException("denied")).when(access).requireOwnProfessional(otherId);
        assertThrows(AccessDeniedException.class, () -> scheduleService.create(
                new ScheduleRequestDTO(otherId, LocalDate.now(), LocalTime.NOON)));
        verifyNoInteractions(professionals);
    }

    @Test void appointmentListsAndLookupEnforceProfessionalOwnership() {
        var own = appointment(AppointmentService.SCHEDULED);
        when(access.role()).thenReturn(Role.PROFESSIONAL);
        when(access.professionalId()).thenReturn(own.getProfessional().getId());
        when(appointments.findAllByProfessional_Id(own.getProfessional().getId())).thenReturn(List.of(own));
        assertEquals(1, appointmentService.findAll().size());
        verify(appointments, never()).findAll();

        when(appointments.findById(own.getId())).thenReturn(Optional.of(own));
        appointmentService.findById(own.getId());
        verify(access).requireOwnProfessional(own.getProfessional().getId());
    }

    @Test void professionalCannotReadOrChangeAnotherAppointment() {
        var other = appointment(AppointmentService.CONFIRMED);
        when(access.role()).thenReturn(Role.PROFESSIONAL);
        when(appointments.findById(other.getId())).thenReturn(Optional.of(other));
        doThrow(new AccessDeniedException("denied")).when(access)
                .requireOwnProfessional(other.getProfessional().getId());
        assertThrows(AccessDeniedException.class, () -> appointmentService.findById(other.getId()));
        var progress = status(AppointmentService.IN_PROGRESS);
        when(statuses.findById(progress.getId())).thenReturn(Optional.of(progress));
        assertThrows(AccessDeniedException.class, () -> appointmentService.updateStatus(other.getId(),
                new AppointmentStatusUpdateRequestDTO(progress.getId())));
    }

    @Test void actorSpecificAppointmentTransitionsAreEnforced() {
        var scheduled = appointment(AppointmentService.SCHEDULED);
        var confirmed = status(AppointmentService.CONFIRMED);
        when(appointments.findById(scheduled.getId())).thenReturn(Optional.of(scheduled));
        when(statuses.findById(confirmed.getId())).thenReturn(Optional.of(confirmed));
        when(access.role()).thenReturn(Role.RECEPTIONIST);
        when(appointments.save(any())).thenAnswer(call -> call.getArgument(0));
        assertEquals(AppointmentService.CONFIRMED, appointmentService.updateStatus(scheduled.getId(),
                new AppointmentStatusUpdateRequestDTO(confirmed.getId())).statusName());

        var professionalAppointment = appointment(AppointmentService.CONFIRMED);
        var progress = status(AppointmentService.IN_PROGRESS);
        when(appointments.findById(professionalAppointment.getId())).thenReturn(Optional.of(professionalAppointment));
        when(statuses.findById(progress.getId())).thenReturn(Optional.of(progress));
        when(access.role()).thenReturn(Role.PROFESSIONAL);
        assertEquals(AppointmentService.IN_PROGRESS, appointmentService.updateStatus(professionalAppointment.getId(),
                new AppointmentStatusUpdateRequestDTO(progress.getId())).statusName());
        verify(access).requireOwnProfessional(professionalAppointment.getProfessional().getId());
    }

    @Test void receptionistCannotStartAttendanceAndProfessionalCannotConfirm() {
        var confirmedAppointment = appointment(AppointmentService.CONFIRMED);
        var progress = status(AppointmentService.IN_PROGRESS);
        when(appointments.findById(confirmedAppointment.getId())).thenReturn(Optional.of(confirmedAppointment));
        when(statuses.findById(progress.getId())).thenReturn(Optional.of(progress));
        when(access.role()).thenReturn(Role.RECEPTIONIST);
        assertThrows(AccessDeniedException.class, () -> appointmentService.updateStatus(confirmedAppointment.getId(),
                new AppointmentStatusUpdateRequestDTO(progress.getId())));

        var scheduledAppointment = appointment(AppointmentService.SCHEDULED);
        var confirmed = status(AppointmentService.CONFIRMED);
        when(appointments.findById(scheduledAppointment.getId())).thenReturn(Optional.of(scheduledAppointment));
        when(statuses.findById(confirmed.getId())).thenReturn(Optional.of(confirmed));
        when(access.role()).thenReturn(Role.PROFESSIONAL);
        assertThrows(AccessDeniedException.class, () -> appointmentService.updateStatus(scheduledAppointment.getId(),
                new AppointmentStatusUpdateRequestDTO(confirmed.getId())));
    }

    @Test void professionalCanCompleteOwnAppointment() {
        var appointment = appointment(AppointmentService.IN_PROGRESS);
        var completed = status(AppointmentService.COMPLETED);
        when(appointments.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(statuses.findById(completed.getId())).thenReturn(Optional.of(completed));
        when(appointments.save(any())).thenAnswer(call -> call.getArgument(0));
        when(access.role()).thenReturn(Role.PROFESSIONAL);
        assertEquals(AppointmentService.COMPLETED, appointmentService.updateStatus(appointment.getId(),
                new AppointmentStatusUpdateRequestDTO(completed.getId())).statusName());
    }

    @Test void medicalRecordReadAndWriteFollowClinicalOwnership() {
        var appointment = appointment(AppointmentService.IN_PROGRESS);
        var record = new MedicalRecord(UUID.randomUUID(), appointment, "notes");
        when(access.role()).thenReturn(Role.PROFESSIONAL);
        when(access.professionalId()).thenReturn(appointment.getProfessional().getId());
        when(records.findAllByAppointment_Professional_Id(appointment.getProfessional().getId()))
                .thenReturn(List.of(record));
        assertEquals(1, recordService.findAll().size());

        when(appointments.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(records.save(any())).thenAnswer(call -> call.getArgument(0));
        recordService.create(new MedicalRecordCreateRequest(appointment.getId(), "new"));
        verify(access).requireOwnProfessional(appointment.getProfessional().getId());

        when(records.findById(record.getId())).thenReturn(Optional.of(record));
        recordService.update(record.getId(), new MedicalRecordUpdateRequest("updated"));
        assertEquals("updated", record.getNotes());
    }

    @Test void receptionistCannotReadAndAdminCannotAuthorMedicalRecord() {
        when(access.role()).thenReturn(Role.RECEPTIONIST);
        assertThrows(AccessDeniedException.class, recordService::findAll);
        when(access.role()).thenReturn(Role.ADMIN);
        assertThrows(AccessDeniedException.class, () -> recordService.create(
                new MedicalRecordCreateRequest(UUID.randomUUID(), "notes")));
        assertThrows(AccessDeniedException.class, () -> recordService.update(
                UUID.randomUUID(), new MedicalRecordUpdateRequest("notes")));
    }

    private static Appointment appointment(String status) {
        var appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setProfessional(professional());
        var patient = new Patient(); patient.setId(UUID.randomUUID()); patient.setName("Patient");
        appointment.setPatient(patient);
        appointment.setDate(LocalDate.now()); appointment.setTime(LocalTime.NOON);
        appointment.setStatus(status(status));
        return appointment;
    }

    private static Professional professional() {
        var professional = new Professional();
        professional.setId(UUID.randomUUID()); professional.setName("Professional");
        return professional;
    }

    private static Status status(String description) {
        return new Status(UUID.randomUUID(), description);
    }
}
