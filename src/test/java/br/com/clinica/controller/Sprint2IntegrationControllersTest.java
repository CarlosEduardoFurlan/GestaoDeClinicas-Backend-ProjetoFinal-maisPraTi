package br.com.clinica.controller;

import br.com.clinica.dto.request.AppointmentRequestDTO;
import br.com.clinica.dto.request.AppointmentStatusUpdateRequestDTO;
import br.com.clinica.dto.request.MedicalRecordCreateRequest;
import br.com.clinica.dto.request.MedicalRecordUpdateRequest;
import br.com.clinica.dto.request.ScheduleRequestDTO;
import br.com.clinica.service.AppointmentService;
import br.com.clinica.service.MedicalRecordService;
import br.com.clinica.service.ScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class Sprint2IntegrationControllersTest {
    private final ScheduleService schedules = mock(ScheduleService.class);
    private final AppointmentService appointments = mock(AppointmentService.class);
    private final MedicalRecordService records = mock(MedicalRecordService.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new ScheduleController(schedules),
                new AppointmentController(appointments), new MedicalRecordController(records)).build();
    }

    @Test
    void scheduleControllerDelegatesListAndCreation() throws Exception {
        var professionalId = UUID.randomUUID();
        var request = new ScheduleRequestDTO(professionalId, LocalDate.of(2027, 1, 10), LocalTime.NOON);
        when(schedules.findAll()).thenReturn(List.of());

        mvc.perform(get("/api/schedules")).andExpect(status().isOk());
        mvc.perform(post("/api/schedules").contentType(MediaType.APPLICATION_JSON).content("""
                {"professionalId":"%s","date":"2027-01-10","time":"12:00:00"}
                """.formatted(professionalId))).andExpect(status().isCreated());

        verify(schedules).findAll();
        verify(schedules).create(request);
    }

    @Test
    void appointmentControllerDelegatesEveryApprovedOperation() throws Exception {
        var id = UUID.randomUUID();
        var patientId = UUID.randomUUID();
        var professionalId = UUID.randomUUID();
        var statusId = UUID.randomUUID();
        var createRequest = new AppointmentRequestDTO(patientId, professionalId,
                LocalDate.of(2027, 1, 10), LocalTime.NOON, "notes");
        when(appointments.findAll()).thenReturn(List.of());

        mvc.perform(get("/api/appointments")).andExpect(status().isOk());
        mvc.perform(get("/api/appointments/{id}", id)).andExpect(status().isOk());
        mvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON).content("""
                {"patientId":"%s","professionalId":"%s","date":"2027-01-10","time":"12:00:00","notes":"notes"}
                """.formatted(patientId, professionalId))).andExpect(status().isCreated());
        mvc.perform(put("/api/appointments/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"statusId\":\"%s\"}".formatted(statusId))).andExpect(status().isOk());
        mvc.perform(delete("/api/appointments/{id}", id)).andExpect(status().isNoContent());

        verify(appointments).findAll();
        verify(appointments).findById(id);
        verify(appointments).create(createRequest);
        verify(appointments).updateStatus(id, new AppointmentStatusUpdateRequestDTO(statusId));
        verify(appointments).cancel(id);
    }

    @Test
    void medicalRecordControllerExposesNoPhysicalDelete() throws Exception {
        var id = UUID.randomUUID();
        var appointmentId = UUID.randomUUID();
        when(records.findAll()).thenReturn(List.of());

        mvc.perform(get("/api/medical-records")).andExpect(status().isOk());
        mvc.perform(get("/api/medical-records/{id}", id)).andExpect(status().isOk());
        mvc.perform(post("/api/medical-records").contentType(MediaType.APPLICATION_JSON)
                .content("{\"appointmentId\":\"%s\",\"notes\":\"texto\"}".formatted(appointmentId)))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/medical-records/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"notes\":\"atualizado\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/medical-records/{id}", id)).andExpect(status().isMethodNotAllowed());

        verify(records).findAll();
        verify(records).findById(id);
        verify(records).create(new MedicalRecordCreateRequest(appointmentId, "texto"));
        verify(records).update(id, new MedicalRecordUpdateRequest("atualizado"));
    }
}
