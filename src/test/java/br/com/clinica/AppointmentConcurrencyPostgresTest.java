package br.com.clinica;

import br.com.clinica.dto.request.AddressRequestDTO;
import br.com.clinica.dto.request.AppointmentRequestDTO;
import br.com.clinica.dto.request.PatientCreateRequest;
import br.com.clinica.dto.request.ProfessionalRequestDTO;
import br.com.clinica.dto.request.ScheduleRequestDTO;
import br.com.clinica.dto.request.SpecialtyRequestDTO;
import br.com.clinica.dto.request.UserRequestDTO;
import br.com.clinica.exception.ConflictException;
import br.com.clinica.repository.ProfessionalRepository;
import br.com.clinica.security.JwtTestSupport;
import br.com.clinica.service.AppointmentService;
import br.com.clinica.service.PatientService;
import br.com.clinica.service.ProfessionalService;
import br.com.clinica.service.ScheduleService;
import br.com.clinica.service.SpecialtyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class AppointmentConcurrencyPostgresTest {
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("security.jwt.secret", JwtTestSupport::randomSecret);
        registry.add("security.jwt.issuer", () -> "clinica-concurrency-test");
        registry.add("security.jwt.expiration-seconds", () -> 900);
    }

    @Autowired SpecialtyService specialtyService;
    @Autowired ProfessionalService professionalService;
    @Autowired PatientService patientService;
    @Autowired ScheduleService scheduleService;
    @Autowired AppointmentService appointmentService;
    @Autowired ProfessionalRepository professionals;

    @Test
    void serializesConcurrentReservationsAndReleasesSlotAfterCancellation() throws Exception {
        authenticateAdmin();
        var suffix = UUID.randomUUID().toString();
        var specialty = specialtyService.create(new SpecialtyRequestDTO("esp-" + suffix, null));
        var email = suffix + "@example.invalid";
        var professionalResponse = professionalService.create(new ProfessionalRequestDTO(
                "Profissional concorrência", "reg-" + suffix.substring(0, 20), "123", "456", email,
                specialty.id(), address(), new UserRequestDTO("Profissional concorrência", email,
                "test-password", "PROFESSIONAL")));
        var professional = professionals.findById(professionalResponse.id()).orElseThrow();
        var patient = patientService.create(new PatientCreateRequest("Paciente concorrência",
                digitsFrom(suffix), LocalDate.of(1990, 1, 1), null, "65999999999", null, "F",
                professional.getAddress().getId()));
        var date = LocalDate.of(2027, 1, 15);
        var time = LocalTime.of(10, 0);
        scheduleService.create(new ScheduleRequestDTO(professional.getId(), date, time));
        var request = new AppointmentRequestDTO(patient.id(), professional.getId(), date, time, "concorrência");

        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        Callable<Object> attempt = () -> {
            authenticateAdmin();
            ready.countDown();
            start.await();
            try {
                return appointmentService.create(request);
            } catch (RuntimeException exception) {
                return exception;
            }
        };

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(attempt);
            var second = executor.submit(attempt);
            ready.await();
            start.countDown();
            var results = java.util.List.of(first.get(), second.get());

            assertEquals(1, results.stream().filter(ConflictException.class::isInstance).count());
            var created = results.stream()
                    .filter(br.com.clinica.dto.response.AppointmentResponseDTO.class::isInstance)
                    .map(br.com.clinica.dto.response.AppointmentResponseDTO.class::cast)
                    .findFirst().orElseThrow();
            assertInstanceOf(ConflictException.class,
                    results.stream().filter(ConflictException.class::isInstance).findFirst().orElseThrow());

            appointmentService.cancel(created.id());
            var replacement = appointmentService.create(request);
            assertEquals("Agendada", replacement.statusName());
        }
    }

    private static void authenticateAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("admin@example.invalid", "n/a",
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private static AddressRequestDTO address() {
        return new AddressRequestDTO("Rua A", 1, null, "Centro", "Cuiabá", "MT", "78000-000");
    }

    private static String digitsFrom(String value) {
        var digits = Integer.toUnsignedString(value.hashCode()) + "00000000000";
        return digits.substring(0, 11);
    }
}
