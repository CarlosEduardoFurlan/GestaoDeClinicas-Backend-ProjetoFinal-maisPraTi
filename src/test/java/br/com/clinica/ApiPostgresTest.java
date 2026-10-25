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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.MediaType;
import br.com.clinica.dto.request.PatientCreateRequest;
import br.com.clinica.dto.request.PatientUpdateRequest;
import br.com.clinica.dto.request.AddressRequestDTO;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;
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
    @Autowired JsonMapper json;
    @Autowired PlatformTransactionManager transactionManager;
    AddressRequestDTO address;
    @BeforeEach void setup() {
        address=new AddressRequestDTO("Rua Teste",1,null,"Centro","Cuiaba","MT","78000-000");
    }
    PatientCreateRequest patientRequest(String cpf, String email) {
        return new PatientCreateRequest("Maria", cpf, LocalDate.of(1990, 5, 20),
                null, "65999999999", email, "F", address);
    }

    String adminToken() {
        return tokenFor("ADMIN");
    }

    String tokenFor(String profile) {
        User admin = new User();
        admin.setName("Admin integração");
        admin.setEmail("admin-integration@example.com");
        admin.setPassword(encoder.encode("integration-password"));
        admin.setProfile(profile);
        users.saveAndFlush(admin);
        return authService.login(new LoginRequestDTO(admin.getEmail(), "integration-password")).accessToken();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPTIONIST"})
    void patientHttpFlowUsesRealPersistenceAndKeepsDeleteDenied(String profile) throws Exception {
        String token = tokenFor(profile);
        var create = patientRequest("12345678901", "http-patient@example.com");
        String body = mvc.perform(post("/api/patients").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(create)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.cpf").value(create.cpf()))
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(json.readTree(body).get("id").asText());
        UUID addressId = UUID.fromString(json.readTree(body).get("addressId").asText());
        mvc.perform(get("/api/patients/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.addressId").value(addressId.toString()))
                .andExpect(jsonPath("$.address.address").value("Rua Teste"));
        var update = new PatientUpdateRequest("Nome atualizado", create.dateBirth(), null, "65999999999",
                "new-http@example.com", "F", new AddressRequestDTO("Rua Atualizada",2,"Sala 1","Centro","Cuiaba","MT","78000-001"));
        mvc.perform(put("/api/patients/" + id).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(update)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Nome atualizado"))
                .andExpect(jsonPath("$.cpf").value(create.cpf()))
                .andExpect(jsonPath("$.addressId").value(addressId.toString()))
                .andExpect(jsonPath("$.address.address").value("Rua Atualizada"));
        assertEquals("new-http@example.com", patients.findById(id).orElseThrow().getEmail());
        mvc.perform(delete("/api/patients/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        assertTrue(patients.existsById(id));
        mvc.perform(get("/api/patients/" + UUID.randomUUID()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/patients").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
    @Test void migrationsAndJpaStart() {
        assertEquals(15, jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class));
        assertEquals(5, jdbc.queryForObject("select count(*) from status where description in "
                + "('Agendada','Confirmada','Em atendimento','Concluída','Cancelada')", Integer.class));
        assertEquals("text", jdbc.queryForObject("select data_type from information_schema.columns where table_name='appointments' and column_name='notes'",String.class));
        assertEquals("NO", jdbc.queryForObject("select is_nullable from information_schema.columns "
                + "where table_name='medical_records' and column_name='notes'", String.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from information_schema.columns where table_name='medical_records' "
                + "and column_name in ('symptoms','diagnosis','prescription')", Integer.class));
        assertEquals(3, jdbc.queryForObject("select count(*) from pg_constraint where conname in ('uc_id_user', 'uc_medical_records', 'uc_schedule') and contype='u'",Integer.class));
        assertEquals(60, jdbc.queryForObject("select character_maximum_length from information_schema.columns where table_name='users' and column_name='password'",Integer.class));
    }
    @Test void patientCrud() throws Exception {
        // Valida o CRUD pelo service; DELETE HTTP permanece bloqueado pela política atual.
        var created = patientService.create(patientRequest("12345678901", "maria@example.com"));
        UUID id = created.id();
        assertEquals("Maria", created.name());
        UUID addressId = patientService.findById(id).addressId();
        assertEquals("Rua Teste", patientService.findById(id).address().address());
        var update = new PatientUpdateRequest("Maria Nova", LocalDate.of(1990, 5, 20),
                null, "65999999999", "maria@example.com", "F",
                new AddressRequestDTO("Rua Nova",2,null,"Centro","Cuiaba","MT","78000-001"));
        var updated = patientService.update(id, update);
        assertEquals("Maria Nova", updated.name()); assertEquals(addressId, updated.addressId());
        assertEquals("Rua Nova", updated.address().address());
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
    @Test void rollsBackNewAddressWhenPatientPersistenceFails() {
        long before = addresses.count();
        var invalid = new PatientCreateRequest("X".repeat(101), "98765432100", LocalDate.of(1990,5,20),
                null,"65999999999",null,"F",address);
        var transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> transaction.executeWithoutResult(ignored -> patientService.create(invalid)));
        assertEquals(before, addresses.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPTIONIST", "PROFESSIONAL"})
    void authenticatedRolesReadOfficialStatusCatalog(String profile) throws Exception {
        mvc.perform(get("/api/statuses").header("Authorization", "Bearer " + tokenFor(profile)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[*].description", org.hamcrest.Matchers.containsInAnyOrder(
                        "Agendada", "Confirmada", "Em atendimento", "Concluída", "Cancelada")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPTIONIST"})
    void authenticatedAdministrativeUserHasNoProfessional(String profile) throws Exception {
        String token = tokenFor(profile);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Admin integração"))
                .andExpect(jsonPath("$.email").value("admin-integration@example.com"))
                .andExpect(jsonPath("$.profile").value(profile))
                .andExpect(jsonPath("$.professionalId").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.password").doesNotExist());
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
        String createdBody = mvc.perform(post("/api/professionals").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.specialtyId").value(specialty.getId().toString()))
                .andExpect(jsonPath("$.address.address").value("Rua A"))
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        var createdJson = json.readTree(createdBody);
        String professionalId = createdJson.get("id").asText();
        var user=users.findByEmail("ana@example.com").orElseThrow();
        assertNotEquals("test-password",user.getPassword()); assertTrue(encoder.matches("test-password",user.getPassword()));
        mvc.perform(get("/api/professionals").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == 'ana@example.com')].userId")
                        .value(org.hamcrest.Matchers.hasItem(user.getId().toString())))
                .andExpect(jsonPath("$[?(@.email == 'ana@example.com')].specialtyId")
                        .value(org.hamcrest.Matchers.hasItem(specialty.getId().toString())));
        mvc.perform(get("/api/professionals/" + professionalId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.address.city").value("Cuiaba"))
                .andExpect(jsonPath("$.password").doesNotExist());
        String updateBody="""
            {"name":"Ana Atualizada","register":"CRM-123","telephone":"6533333333","cellPhone":"65999999999",
             "email":"ana@example.com","specialtyId":"%s",
             "address":{"address":"%s","number":%s,"district":"%s","city":"%s","uf":"%s","zipCode":"%s"}}
            """.formatted(createdJson.get("specialtyId").asText(),
                createdJson.get("address").get("address").asText(), createdJson.get("address").get("number").asInt(),
                createdJson.get("address").get("district").asText(), createdJson.get("address").get("city").asText(),
                createdJson.get("address").get("uf").asText(), createdJson.get("address").get("zipCode").asText());
        mvc.perform(put("/api/professionals/" + professionalId).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Ana Atualizada"));
        mvc.perform(post("/api/professionals").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"ana@example.com","password":"test-password"}
                """)).andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.password").doesNotExist());
        String professionalToken = authService.login(new LoginRequestDTO("ana@example.com", "test-password")).accessToken();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + professionalToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(user.getId().toString()))
                .andExpect(jsonPath("$.professionalId").value(professionalId))
                .andExpect(jsonPath("$.profile").value("PROFESSIONAL"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }
}
