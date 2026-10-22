package br.com.clinica.service;

import br.com.clinica.dto.request.MedicalRecordCreateRequest;
import br.com.clinica.dto.request.MedicalRecordUpdateRequest;
import br.com.clinica.entity.Appointment;
import br.com.clinica.entity.MedicalRecord;
import br.com.clinica.exception.ConflictException;
import br.com.clinica.exception.ResourceNotFoundException;
import br.com.clinica.repository.AppointmentRepository;
import br.com.clinica.repository.MedicalRecordRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MedicalRecordServiceTest {
    private final MedicalRecordRepository records = mock(MedicalRecordRepository.class);
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final MedicalRecordService service = new MedicalRecordService(records, appointments);

    @Test
    void createsOneSimplifiedRecordForExistingAppointment() {
        var appointment = appointment();
        var request = new MedicalRecordCreateRequest(appointment.getId(), "Evolução clínica em texto livre");
        when(appointments.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(records.save(any())).thenAnswer(invocation -> {
            MedicalRecord record = invocation.getArgument(0);
            record.setId(UUID.randomUUID());
            return record;
        });

        var response = service.create(request);

        assertEquals(appointment.getId(), response.appointmentId());
        assertEquals(request.notes(), response.notes());
    }

    @Test
    void rejectsMissingAppointment() {
        var request = new MedicalRecordCreateRequest(UUID.randomUUID(), "notes");
        assertThrows(ResourceNotFoundException.class, () -> service.create(request));
        verify(records, never()).save(any());
    }

    @Test
    void rejectsSecondRecordForSameAppointment() {
        var appointment = appointment();
        when(appointments.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(records.existsByAppointment_Id(appointment.getId())).thenReturn(true);
        assertThrows(ConflictException.class,
                () -> service.create(new MedicalRecordCreateRequest(appointment.getId(), "notes")));
        verify(records, never()).save(any());
    }

    @Test
    void listsFindsAndUpdatesNotesWithoutChangingAppointment() {
        var appointment = appointment();
        var record = new MedicalRecord(UUID.randomUUID(), appointment, "antes");
        when(records.findAll()).thenReturn(List.of(record));
        when(records.findById(record.getId())).thenReturn(Optional.of(record));
        when(records.save(record)).thenReturn(record);

        assertEquals(record.getId(), service.findAll().getFirst().id());
        assertEquals(appointment.getId(), service.findById(record.getId()).appointmentId());
        var updated = service.update(record.getId(), new MedicalRecordUpdateRequest("depois"));
        assertEquals("depois", updated.notes());
        assertEquals(appointment.getId(), updated.appointmentId());
    }

    private static Appointment appointment() {
        var appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        return appointment;
    }
}
