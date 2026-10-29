package br.com.clinica;

import br.com.clinica.dto.request.*;
import br.com.clinica.security.JwtTestSupport;
import br.com.clinica.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** No test transaction: the controller must observe the service's flush/commit failure. */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class UserDeletePostgresTest {
    private static final String SECRET = JwtTestSupport.randomSecret();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("security.jwt.secret", () -> SECRET);
    }
    @Autowired MockMvc mvc;
    @Autowired UserService users;
    @Autowired AuthService auth;
    @Autowired ProfessionalService professionals;
    @Autowired SpecialtyService specialties;
    @Autowired JdbcTemplate jdbc;
    UUID adminId, userId, professionalId, specialtyId, addressId, scheduleId;
    String token;

    @BeforeEach void setup() {
        String email = UUID.randomUUID() + "@example.test";
        var admin = users.create(new UserRequestDTO("Delete admin", email, "test-password", "ADMIN"));
        adminId = admin.id();
        token = auth.login(new LoginRequestDTO(email, "test-password")).accessToken();
    }
    @AfterEach void cleanup() {
        if (scheduleId != null) jdbc.update("delete from schedule where id = ?", scheduleId);
        if (professionalId != null) jdbc.update("delete from professionals where id = ?", professionalId);
        if (userId != null) jdbc.update("delete from users where id = ?", userId);
        if (addressId != null) jdbc.update("delete from address where id = ?", addressId);
        if (specialtyId != null) jdbc.update("delete from specialties where id = ?", specialtyId);
        if (adminId != null) jdbc.update("delete from users where id = ?", adminId);
    }
    @Test void dependentUserReturnsConflictAndPreservesEntireChain() throws Exception {
        createProfessional();
        scheduleId = UUID.randomUUID();
        jdbc.update("insert into schedule(id, id_professional, date, time) values (?, ?, ?, ?)", scheduleId, professionalId, LocalDate.of(2030, 1, 2), LocalTime.NOON);
        var result = mvc.perform(delete("/api/users/{id}", userId).header("Authorization", "Bearer " + token)).andReturn();
        assertEquals(409, result.getResponse().getStatus(), "Dependent User DELETE must be 409, never generic 500");
        assertEquals(1, count("users", userId));
        assertEquals(1, count("professionals", professionalId));
        assertEquals(1, count("schedule", scheduleId));
        assertEquals(userId, jdbc.queryForObject("select id_user from professionals where id = ?", UUID.class, professionalId));
        assertEquals(professionalId, jdbc.queryForObject("select id_professional from schedule where id = ?", UUID.class, scheduleId));
    }
    private void createProfessional() {
        specialtyId = specialties.create(new SpecialtyRequestDTO(UUID.randomUUID().toString(), null)).id();
        var professional = professionals.create(new ProfessionalRequestDTO("Dependent professional", UUID.randomUUID().toString().substring(0, 20),
                "123", "123", "contact@example.test", specialtyId,
                new AddressRequestDTO("Rua Teste", 1, null, "Centro", "Cuiaba", "MT", "78000-000"),
                new UserRequestDTO("Dependent user", UUID.randomUUID() + "@example.test", "test-password", "PROFESSIONAL")));
        professionalId = professional.id(); userId = professional.userId();
        addressId = jdbc.queryForObject("select id_address from professionals where id = ?", UUID.class, professionalId);
    }
    @Test void userWithoutBlockingDependenciesUsesExistingDatabaseCascade() throws Exception {
        createProfessional();
        mvc.perform(delete("/api/users/{id}", userId).header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
        assertEquals(0, count("users", userId));
        assertEquals(0, count("professionals", professionalId));
        assertEquals(1, count("address", addressId));
    }
    @Test void independentUserReturnsNoContentAndIsRemoved() throws Exception {
        userId = users.create(new UserRequestDTO("Disposable user", UUID.randomUUID() + "@example.test", "test-password", "RECEPTIONIST")).id();
        mvc.perform(delete("/api/users/{id}", userId).header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
        assertEquals(0, count("users", userId));
    }
    @Test void unknownUserReturnsNotFound() throws Exception {
        mvc.perform(delete("/api/users/{id}", UUID.randomUUID()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }
    private int count(String table, UUID id) {
        return jdbc.queryForObject("select count(*) from " + table + " where id = ?", Integer.class, id);
    }
}
