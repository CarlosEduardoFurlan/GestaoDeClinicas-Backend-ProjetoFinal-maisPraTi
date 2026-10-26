package br.com.clinica;

import br.com.clinica.dto.request.*;
import br.com.clinica.exception.ConflictException;
import br.com.clinica.repository.ScheduleRepository;
import br.com.clinica.security.JwtTestSupport;
import br.com.clinica.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Fixtures commit before requests; no outer test transaction hides constraints or HTTP errors. */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class ScheduleDuplicatePostgresTest {
    private static final String SECRET = JwtTestSupport.randomSecret();
    private static final LocalDate DATE = LocalDate.of(2030, 1, 7);
    private static final LocalTime TIME = LocalTime.of(10, 15);
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("security.jwt.secret", () -> SECRET);
    }
    @Autowired MockMvc mvc;
    @Autowired UserService users;
    @Autowired AuthService auth;
    @Autowired ProfessionalService professionals;
    @Autowired SpecialtyService specialties;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean ScheduleRepository schedules;
    final List<UUID> userIds = new ArrayList<>(), professionalIds = new ArrayList<>(), addressIds = new ArrayList<>();
    UUID specialtyId;
    String adminToken, receptionToken, professionalToken;

    @BeforeEach void setup() {
        adminToken = account("ADMIN"); receptionToken = account("RECEPTIONIST");
        specialtyId = specialties.create(new SpecialtyRequestDTO(UUID.randomUUID().toString(), null)).id();
        for (int index = 0; index < 2; index++) {
            String email = UUID.randomUUID() + "@example.test";
            var p = professionals.create(new ProfessionalRequestDTO("Schedule professional", UUID.randomUUID().toString().substring(0, 20),
                    "123", "456", email, specialtyId,
                    new AddressRequestDTO("Rua Teste", 1, null, "Centro", "Cuiaba", "MT", "78000-000"),
                    new UserRequestDTO("Schedule user", email, "test-password", "PROFESSIONAL")));
            professionalIds.add(p.id()); userIds.add(p.userId());
            addressIds.add(jdbc.queryForObject("select id_address from professionals where id = ?", UUID.class, p.id()));
            if (index == 0) professionalToken = auth.login(new LoginRequestDTO(email, "test-password")).accessToken();
        }
    }
    @AfterEach void cleanup() {
        for (var id : professionalIds) { jdbc.update("delete from schedule where id_professional = ?", id); jdbc.update("delete from professionals where id = ?", id); }
        for (var id : userIds) jdbc.update("delete from users where id = ?", id);
        for (var id : addressIds) jdbc.update("delete from address where id = ?", id);
        if (specialtyId != null) jdbc.update("delete from specialties where id = ?", specialtyId);
    }
    @Test void duplicateSequentiallyReturnsConflict() throws Exception {
        assertEquals(201, create(professionalIds.getFirst(), adminToken).getResponse().getStatus());
        var duplicate = create(professionalIds.getFirst(), adminToken);
        assertEquals(409, duplicate.getResponse().getStatus(), "Sequential duplicate must be 409, not 400");
        assertInstanceOf(ConflictException.class, duplicate.getResolvedException());
        jsonPath("$.message").value("Horário já cadastrado para este profissional.").match(duplicate);
        assertEquals(1, count());
    }
    @Test void realPostgresConstraintReturnsConflictWhenPrecheckLosesRace() throws Exception {
        assertEquals(201, create(professionalIds.getFirst(), adminToken).getResponse().getStatus());
        // Simulate only the stale pre-check. INSERT, UNIQUE V13, rollback and exception translation remain real.
        doReturn(false).when(schedules).existsByProfessional_IdAndDateAndTime(professionalIds.getFirst(), DATE, TIME);
        var duplicate = create(professionalIds.getFirst(), adminToken);
        assertEquals(409, duplicate.getResponse().getStatus());
        assertInstanceOf(DataIntegrityViolationException.class, duplicate.getResolvedException());
        assertEquals(1, count());
    }
    @Test void concurrentRequestsCreateExactlyOneSchedule() throws Exception {
        var ready = new CountDownLatch(2); var start = new CountDownLatch(1);
        Callable<Integer> attempt = () -> {
            ready.countDown(); assertTrue(start.await(10, TimeUnit.SECONDS));
            return create(professionalIds.getFirst(), adminToken).getResponse().getStatus();
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(attempt); var second = executor.submit(attempt);
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            assertEquals(List.of(201, 409), java.util.stream.Stream.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)).sorted().toList());
        }
        assertEquals(1, count());
    }
    @Test void unknownProfessionalReturnsNotFound() throws Exception {
        assertEquals(404, create(UUID.randomUUID(), adminToken).getResponse().getStatus());
    }
    @Test void professionalCanCreateOwnSlotButCannotCreateForAnother() throws Exception {
        assertEquals(201, create(professionalIds.getFirst(), professionalToken).getResponse().getStatus());
        assertEquals(403, create(professionalIds.getLast(), professionalToken).getResponse().getStatus());
        assertEquals(1, count());
    }
    @Test void receptionistCannotCreateSchedule() throws Exception {
        assertEquals(403, create(professionalIds.getFirst(), receptionToken).getResponse().getStatus());
        assertEquals(0, count());
    }
    @Test void invalidDtoStillReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/schedules").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.details").isArray());
    }
    private String account(String profile) {
        String email = UUID.randomUUID() + "@example.test";
        userIds.add(users.create(new UserRequestDTO("Schedule test", email, "test-password", profile)).id());
        return auth.login(new LoginRequestDTO(email, "test-password")).accessToken();
    }
    private MvcResult create(UUID professionalId, String token) throws Exception {
        return mvc.perform(post("/api/schedules").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"professionalId":"%s","date":"2030-01-07","time":"10:15:00"}
                """.formatted(professionalId))).andReturn();
    }
    private int count() {
        return jdbc.queryForObject("select count(*) from schedule where id_professional in (?, ?)", Integer.class, professionalIds.getFirst(), professionalIds.getLast());
    }
}
