package br.com.clinica;

import br.com.clinica.dto.request.*;
import br.com.clinica.entity.*;
import br.com.clinica.repository.*;
import br.com.clinica.security.JwtTestSupport;
import br.com.clinica.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class UsersProfessionalsPostgresTest {
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("security.jwt.secret", JwtTestSupport::randomSecret);
        registry.add("security.jwt.issuer", () -> "clinica-test");
        registry.add("security.jwt.expiration-seconds", () -> 900);
    }
    @Autowired UserService userService;
    @Autowired ProfessionalService professionalService;
    @Autowired SpecialtyService specialtyService;
    @Autowired UserRepository users;
    @Autowired ProfessionalRepository professionals;
    @Autowired ScheduleRepository schedules;
    @Autowired PasswordEncoder encoder;
    @Autowired jakarta.persistence.EntityManager entityManager;

    @Test void userCreationAndUpdatePersistWithBcryptAndStableEmail() {
        String email = UUID.randomUUID() + "@example.com";
        var created = userService.create(new UserRequestDTO("Teste", email, "test-password", "ADMIN"));
        users.flush();
        assertNotNull(created.id());
        assertTrue(encoder.matches("test-password", users.findById(created.id()).orElseThrow().getPassword()));
        var updated = userService.update(created.id(), new UserUpdateRequestDTO("Novo", "new-password", "RECEPTIONIST"));
        users.flush();
        assertEquals(email, updated.email());
        assertTrue(encoder.matches("new-password", users.findById(created.id()).orElseThrow().getPassword()));
    }

    @Test void professionalWithoutDependenciesCanBeDeletedAndKeepsUser() {
        var p = createProfessional();
        var userId = p.getUser().getId();
        professionalService.delete(p.getId()); professionals.flush();
        assertFalse(professionals.existsById(p.getId()));
        assertTrue(users.existsById(userId));
    }

    @Test void scheduleForeignKeyPreventsProfessionalDeletion() {
        var p = createProfessional();
        var slot = new Schedule(); slot.setProfessional(p); slot.setDate(LocalDate.of(2026, 1, 1)); slot.setTime(LocalTime.NOON);
        schedules.saveAndFlush(slot);
        // Simula uma requisição posterior: dependências já persistidas fora do contexto da exclusão.
        entityManager.clear();
        professionalService.delete(p.getId());
        assertThrows(DataIntegrityViolationException.class, () -> professionals.flush());
    }

    @Test void specialtyForeignKeyPreventsDeletion() {
        var p = createProfessional();
        var specialtyId = p.getSpecialty().getId();
        entityManager.clear();
        assertThrows(DataIntegrityViolationException.class, () -> specialtyService.delete(specialtyId));
    }

    @Test void specialtyCrudAndCaseSensitiveUniqueNamesMatchPostgres() {
        String name = "Specialty-" + UUID.randomUUID().toString().substring(0, 8);
        var created = specialtyService.create(new SpecialtyRequestDTO(name, "description"));
        assertEquals(name, specialtyService.findById(created.id()).name());
        assertTrue(specialtyService.findAll().stream().anyMatch(s -> s.id().equals(created.id())));
        var second = specialtyService.create(new SpecialtyRequestDTO(name.toLowerCase(), null));
        assertThrows(br.com.clinica.exception.BusinessException.class,
                () -> specialtyService.update(created.id(), new SpecialtyRequestDTO(name.toLowerCase(), null)));
        specialtyService.delete(second.id());
        assertEquals("updated", specialtyService.update(created.id(), new SpecialtyRequestDTO(name, "updated")).description());
        specialtyService.delete(created.id());
    }

    private Professional createProfessional() {
        var specialty = specialtyService.create(new SpecialtyRequestDTO(UUID.randomUUID().toString(), null));
        String email = UUID.randomUUID() + "@example.com";
        var created = professionalService.create(new ProfessionalRequestDTO("Teste", UUID.randomUUID().toString().substring(0, 20),
                "123", "123", email, specialty.id(),
                new AddressRequestDTO("Rua A", 1, null, "Centro", "Cuiaba", "MT", "78000-000"),
                new UserRequestDTO("Teste", email, "test-password", "PROFESSIONAL")));
        professionals.flush();
        return professionals.findById(created.id()).orElseThrow();
    }
}
