package br.com.clinica;

import br.com.clinica.dto.request.*;
import br.com.clinica.entity.*;
import br.com.clinica.repository.*;
import br.com.clinica.service.*;
import br.com.clinica.security.JwtTestSupport;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class ScheduleMedicalRecordPostgresTest {
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("security.jwt.secret", JwtTestSupport::randomSecret);
        registry.add("security.jwt.issuer", () -> "clinica-schema-test");
        registry.add("security.jwt.expiration-seconds", () -> 900);
    }
    @Autowired SpecialtyService specialtyService;
    @Autowired ProfessionalService professionalService;
    @Autowired ScheduleService scheduleService;
    @Autowired AppointmentService appointmentService;
    @Autowired PatientService patientService;
    @Autowired ProfessionalRepository professionals;
    @Autowired PatientRepository patients;
    @Autowired ScheduleRepository schedules;
    @Autowired AppointmentRepository appointments;
    @Autowired MedicalRecordRepository records;
    @Autowired StatusRepository statuses;
    @Autowired jakarta.persistence.EntityManager entityManager;
    private Professional professional;
    private Patient patient;
    private Status status;
    private final LocalDate date = LocalDate.of(2000, 1, 1);
    private final LocalTime time = LocalTime.NOON;

    @BeforeEach void fixtures() {
        var specialty = specialtyService.create(new SpecialtyRequestDTO(UUID.randomUUID().toString(), null));
        String email = UUID.randomUUID() + "@example.com";
        var p = professionalService.create(new ProfessionalRequestDTO("Ana", UUID.randomUUID().toString().substring(0, 20),
                "123", "123", email, specialty.id(), new AddressRequestDTO("Rua A",1,null,"Centro","Cuiaba","MT","78000-000"),
                new UserRequestDTO("Ana",email,"test-password","PROFESSIONAL")));
        professional = professionals.findById(p.id()).orElseThrow();
        var patientDto = patientService.create(new PatientCreateRequest("Maria", "12345678901", LocalDate.of(1990,1,1),
                null,"123",null,"F",professional.getAddress().getId()));
        patient = patients.findById(patientDto.id()).orElseThrow();
        status = statuses.saveAndFlush(new Status(null, "schema-test-fixture"));
    }

    @Test void persistsAndListsScheduleWithoutTemporalRestriction() {
        var created = scheduleService.create(new ScheduleRequestDTO(professional.getId(), date, time));
        schedules.flush(); entityManager.clear();
        var found = scheduleService.findAll().stream().filter(s -> s.id().equals(created.id())).findFirst().orElseThrow();
        assertEquals(date, found.date()); assertNotNull(found.created());
        assertThrows(br.com.clinica.exception.BusinessException.class,
                () -> scheduleService.create(new ScheduleRequestDTO(professional.getId(), date, time)));
    }

    @Test void databaseRejectsDuplicateScheduleEvenBypassingService() {
        schedules.saveAndFlush(new Schedule(null, professional, date, time, null));
        assertThrows(DataIntegrityViolationException.class,
                () -> schedules.saveAndFlush(new Schedule(null, professional, date, time, null)));
    }

    @Test void appointmentCanExistWithoutMedicalRecordAndBeListed() {
        var appointment = appointment();
        entityManager.clear();
        assertEquals(0, records.count());
        var found = appointmentService.findAll().stream().filter(a -> a.id().equals(appointment.getId())).findFirst().orElseThrow();
        assertEquals(patient.getId(), found.patientId()); assertEquals(status.getId(), found.statusId());
        assertEquals(501, found.notes().length());
    }

    @Test void databaseRejectsSecondMedicalRecordForSameAppointment() {
        var appointment = appointment();
        records.saveAndFlush(new MedicalRecord(null, appointment, "symptoms", "diagnosis", "prescription"));
        assertThrows(DataIntegrityViolationException.class,
                () -> records.saveAndFlush(new MedicalRecord(null, appointment, "other", "other", "other")));
    }

    private Appointment appointment() {
        return appointments.saveAndFlush(new Appointment(null, patient, professional, date, time, status, null, "x".repeat(501)));
    }
}
