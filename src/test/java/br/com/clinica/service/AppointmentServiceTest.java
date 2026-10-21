package br.com.clinica.service;

import br.com.clinica.entity.*;
import br.com.clinica.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppointmentServiceTest {
    @Test void listsAppointmentsWithRealRelationshipIdentifiers() {
        var repository = mock(AppointmentRepository.class);
        var patient = new Patient(); patient.setId(UUID.randomUUID()); patient.setName("Maria");
        var professional = new Professional(); professional.setId(UUID.randomUUID()); professional.setName("Ana");
        var status = new Status(UUID.randomUUID(), "fixture");
        var appointment = new Appointment(); appointment.setId(UUID.randomUUID());
        appointment.setPatient(patient); appointment.setProfessional(professional); appointment.setStatus(status);
        appointment.setNotes("x".repeat(501));
        when(repository.findAll()).thenReturn(List.of(appointment));
        var result = new AppointmentService(repository).findAll().getFirst();
        assertEquals(patient.getId(), result.patientId()); assertEquals(professional.getId(), result.professionalId());
        assertEquals(status.getId(), result.statusId()); assertEquals(501, result.notes().length());
    }
}
