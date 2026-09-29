package br.com.clinica;

import br.com.clinica.entity.*;
import br.com.clinica.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.MediaType;
import br.com.clinica.dto.request.PatientCreateRequest;
import br.com.clinica.dto.request.PatientUpdateRequest;
import br.com.clinica.dto.request.LoginRequestDTO;
import br.com.clinica.exception.BusinessException;
import br.com.clinica.exception.ResourceNotFoundException;
import br.com.clinica.service.PatientService;
import br.com.clinica.service.AuthService;
import br.com.clinica.security.JwtTestSupport;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named="RUN_POSTGRES_TESTS", matches="true")
class ApiPostgresTest {
    private static final String TEST_SECRET = JwtTestSupport.randomSecret();
    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("security.jwt.secret", () -> TEST_SECRET);
        registry.add("security.jwt.issuer", () -> "clinica-postgres-test");
        registry.add("security.jwt.expiration-seconds", () -> 900);
    }
    @Autowired MockMvc mvc;
    @Autowired AddressRepository addresses;
    @Autowired SpecialtyRepository specialties;
    @Autowired UserRepository users;
    @Autowired PatientRepository patients;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired PatientService patientService;
    @Autowired AuthService authService;
    Address address;
    @BeforeEach void setup() {
        address=new Address(); address.setAddress("Rua Teste"); address.setNumber(1);
        address.setDistrict("Centro"); address.setCity("Cuiaba"); address.setUf("MT"); address.setZipCode("78000-000");
        address=addresses.saveAndFlush(address);
    }
    PatientCreateRequest patientRequest(String cpf, String email) {
        return new PatientCreateRequest("Maria", cpf, LocalDate.of(1990, 5, 20),
                null, "65999999999", email, "F", address.getId());
    }

    String adminToken() {
        User admin = new User();
        admin.setName("Admin integração");
        admin.setEmail("admin-integration@example.com");
        admin.setPassword(encoder.encode("integration-password"));
        admin.setProfile("ADMIN");
        users.saveAndFlush(admin);
        return authService.login(new LoginRequestDTO(admin.getEmail(), "integration-password")).accessToken();
    }
    @Test void migrationsAndJpaStart() {
        assertEquals(13, jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class));
        assertEquals("text", jdbc.queryForObject("select data_type from information_schema.columns where table_name='appointments' and column_name='notes'",String.class));
        assertEquals(3, jdbc.queryForObject("select count(*) from pg_constraint where conname in ('uc_id_user', 'uc_medical_records', 'uc_schedule') and contype='u'",Integer.class));
        assertEquals(60, jdbc.queryForObject("select character_maximum_length from information_schema.columns where table_name='users' and column_name='password'",Integer.class));
    }
    @Test void patientCrud() throws Exception {
        // Valida o CRUD pelo service; DELETE HTTP permanece bloqueado pela política atual.
        var created = patientService.create(patientRequest("12345678901", "maria@example.com"));
        UUID id = created.id();
        assertEquals("Maria", created.name());
        assertEquals(address.getId(), patientService.findById(id).addressId());
        var update = new PatientUpdateRequest("Maria Nova", LocalDate.of(1990, 5, 20),
                null, "65999999999", "maria@example.com", "F", address.getId());
        assertEquals("Maria Nova", patientService.update(id, update).name());
        assertTrue(patientService.findAll().stream().anyMatch(patient -> patient.id().equals(id)));
        mvc.perform(get("/api/patients").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.hasItem(id.toString())));
        patientService.delete(id);
        assertThrows(ResourceNotFoundException.class, () -> patientService.findById(id));
    }
    @Test void invalidInputUses400() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.details").isArray());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/professionals").header("Authorization", "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
    }
    @Test void duplicatesAreBusinessErrors() throws Exception {
        patientService.create(patientRequest("12345678901", "maria@example.com"));
        assertThrows(BusinessException.class,
                () -> patientService.create(patientRequest("12345678901", "outra@example.com")));
        assertThrows(BusinessException.class,
                () -> patientService.create(patientRequest("98765432100", "maria@example.com")));
    }
    @Test void professionalPersistsHashedPassword() throws Exception {
        String token = adminToken();
        Specialty specialty=new Specialty(); specialty.setName("Clinica geral"); specialty=specialties.saveAndFlush(specialty);
        String body="""
            {"name":"Ana", "register":"CRM-123", "telephone":"6533333333", "cellPhone":"65999999999",
             "email":"ana@example.com", "specialtyId":"%s",
             "address":{"address":"Rua A","number":1,"district":"Centro","city":"Cuiaba","uf":"MT","zipCode":"78000-000"},
             "user":{"name":"Ana","email":"ana@example.com","password":"test-password","profile":"PROFESSIONAL"}}
            """.formatted(specialty.getId());
        mvc.perform(post("/api/professionals").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.user.password").doesNotExist());
        var user=users.findByEmail("ana@example.com").orElseThrow();
        assertNotEquals("test-password",user.getPassword()); assertTrue(encoder.matches("test-password",user.getPassword()));
        mvc.perform(get("/api/professionals").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == 'ana@example.com')].userId")
                        .value(org.hamcrest.Matchers.hasItem(user.getId().toString())));
        mvc.perform(post("/api/professionals").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"ana@example.com","password":"test-password"}
                """)).andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.password").doesNotExist());
    }
}
