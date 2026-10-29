package br.com.clinica;

import br.com.clinica.entity.*;
import br.com.clinica.repository.*;
import br.com.clinica.security.JwtTestSupport;
import br.com.clinica.service.AppointmentService;
import br.com.clinica.service.MedicalRecordService;
import br.com.clinica.service.ScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class OwnershipPostgresTest {
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("security.jwt.secret", JwtTestSupport::randomSecret);
        registry.add("security.jwt.issuer", () -> "clinica-ownership-postgres-test");
        registry.add("security.jwt.expiration-seconds", () -> 900);
    }

    @Autowired UserRepository users;
    @Autowired AddressRepository addresses;
    @Autowired SpecialtyRepository specialties;
    @Autowired ProfessionalRepository professionals;
    @Autowired PatientRepository patients;
    @Autowired ScheduleRepository schedules;
    @Autowired AppointmentRepository appointments;
    @Autowired StatusRepository statuses;
    @Autowired MedicalRecordRepository records;
    @Autowired ScheduleService scheduleService;
    @Autowired AppointmentService appointmentService;
    @Autowired MedicalRecordService recordService;

    Professional own;
    Professional other;
    Appointment ownAppointment;
    Appointment otherAppointment;
    MedicalRecord otherRecord;

    @BeforeEach void setup() {
        var address = new Address();
        address.setAddress("Rua Ownership"); address.setNumber(1); address.setDistrict("Centro");
        address.setCity("Cuiabá"); address.setUf("MT"); address.setZipCode("78000-000");
        address = addresses.save(address);
        var specialty = new Specialty(); specialty.setName("Ownership-" + UUID.randomUUID());
        specialty = specialties.save(specialty);
        own = professional("own-" + UUID.randomUUID() + "@example.invalid", address, specialty);
        other = professional("other-" + UUID.randomUUID() + "@example.invalid", address, specialty);
        var patient = new Patient(); patient.setName("Patient"); patient.setCpf(digits());
        patient.setDateBirth(LocalDate.of(1990, 1, 1)); patient.setCellPhone("65999999999");
        patient.setGender("F"); patient.setAddress(address); patient = patients.save(patient);
        var scheduled = statuses.findByDescription("Agendada").orElseThrow();
        ownAppointment = appointment(own, patient, scheduled, LocalTime.of(9, 0));
        otherAppointment = appointment(other, patient, scheduled, LocalTime.of(10, 0));
        records.save(new MedicalRecord(null, ownAppointment, "own notes"));
        otherRecord = records.save(new MedicalRecord(null, otherAppointment, "other notes"));
        schedules.save(new Schedule(null, own, LocalDate.of(2027, 2, 1), LocalTime.of(9, 0), null));
        schedules.save(new Schedule(null, other, LocalDate.of(2027, 2, 1), LocalTime.of(10, 0), null));
        authenticate(own.getUser().getEmail());
    }

    @Test void realQueriesFilterOwnResourcesAndDenyForeignIdentifiers() {
        assertEquals(List.of(own.getId()), scheduleService.findAll().stream()
                .map(response -> response.professionalId()).toList());
        assertEquals(List.of(ownAppointment.getId()), appointmentService.findAll().stream()
                .map(response -> response.id()).toList());
        assertEquals(1, recordService.findAll().size());
        assertThrows(AccessDeniedException.class, () -> appointmentService.findById(otherAppointment.getId()));
        assertThrows(AccessDeniedException.class, () -> recordService.findById(otherRecord.getId()));
    }

    private Professional professional(String email, Address address, Specialty specialty) {
        var user = new User(); user.setName(email); user.setEmail(email); user.setPassword("x".repeat(60));
        user.setProfile("PROFESSIONAL"); user = users.save(user);
        var professional = new Professional(); professional.setName(email);
        professional.setRegister(UUID.randomUUID().toString().substring(0, 30));
        professional.setTelephone("6533333333"); professional.setCellPhone("65999999999");
        professional.setEmail(email); professional.setAddress(address); professional.setSpecialty(specialty);
        professional.setUser(user); return professionals.save(professional);
    }

    private Appointment appointment(Professional professional, Patient patient, Status status, LocalTime time) {
        var appointment = new Appointment(); appointment.setProfessional(professional); appointment.setPatient(patient);
        appointment.setStatus(status); appointment.setDate(LocalDate.of(2027, 2, 1)); appointment.setTime(time);
        return appointments.save(appointment);
    }

    private static void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(email, "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_PROFESSIONAL"))));
    }

    private static String digits() {
        return String.format("%011d", Math.abs(UUID.randomUUID().hashCode()) % 100_000_000_000L);
    }
}
