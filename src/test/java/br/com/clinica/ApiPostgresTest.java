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
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named="RUN_POSTGRES_TESTS", matches="true")
class ApiPostgresTest {
    @Autowired MockMvc mvc;
    @Autowired AddressRepository addresses;
    @Autowired SpecialtyRepository specialties;
    @Autowired UserRepository users;
    @Autowired PatientRepository patients;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    Address address;
    @BeforeEach void setup() {
        address=new Address(); address.setAddress("Rua Teste"); address.setNumber(1);
        address.setDistrict("Centro"); address.setCity("Cuiaba"); address.setUf("MT"); address.setZipCode("78000-000");
        address=addresses.saveAndFlush(address);
    }
    String payload() { return """
        {"name":"Maria", "cpf":"12345678901", "dateBirth":"1990-05-20", "cellPhone":"65999999999",
        "email":"maria@example.com", "gender":"F", "addressId":"%s"}
        """.formatted(address.getId()); }
    @Test void migrationsAndJpaStart() {
        assertEquals(13, jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class));
        assertEquals("text", jdbc.queryForObject("select data_type from information_schema.columns where table_name='appointments' and column_name='notes'",String.class));
        assertEquals(3, jdbc.queryForObject("select count(*) from pg_constraint where conname in ('uc_id_user', 'uc_medical_records', 'uc_schedule') and contype='u'",Integer.class));
        assertEquals(60, jdbc.queryForObject("select character_maximum_length from information_schema.columns where table_name='users' and column_name='password'",Integer.class));
    }
    @Test void patientCrud() throws Exception {
        mvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON).content(payload())).andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Maria"));
        UUID id=patients.findAll().getFirst().getId();
        mvc.perform(get("/api/patients/{id}",id)).andExpect(status().isOk()).andExpect(jsonPath("$.addressId").value(address.getId().toString()));
        mvc.perform(put("/api/patients/{id}",id).contentType(MediaType.APPLICATION_JSON).content(payload().replace("Maria","Maria Nova"))).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Maria Nova"));
        mvc.perform(get("/api/patients")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(delete("/api/patients/{id}",id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/patients/{id}",id)).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }
    @Test void invalidInputUses400() throws Exception {
        mvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details").isArray());
        mvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON).content("{")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/patients/not-a-uuid")).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/patients")).andExpect(status().isMethodNotAllowed());
    }
    @Test void duplicatesAreBusinessErrors() throws Exception {
        mvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON).content(payload())).andExpect(status().isCreated());
        mvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON).content(payload())).andExpect(status().isBadRequest());
        mvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON).content(payload().replace("12345678901","98765432100"))).andExpect(status().isBadRequest());
    }
    @Test void professionalPersistsHashedPassword() throws Exception {
        Specialty specialty=new Specialty(); specialty.setName("Clinica geral"); specialty=specialties.saveAndFlush(specialty);
        String body="""
            {"name":"Ana", "register":"CRM-123", "telephone":"6533333333", "cellPhone":"65999999999",
             "email":"ana@example.com", "specialtyId":"%s",
             "address":{"address":"Rua A","number":1,"district":"Centro","city":"Cuiaba","uf":"MT","zipCode":"78000-000"},
             "user":{"name":"Ana","email":"ana@example.com","password":"test-password","profile":"PROFESSIONAL"}}
            """.formatted(specialty.getId());
        mvc.perform(post("/api/professionals").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andExpect(jsonPath("$.password").doesNotExist());
        var user=users.findAll().getFirst(); assertNotEquals("test-password",user.getPassword()); assertTrue(encoder.matches("test-password",user.getPassword()));
        mvc.perform(get("/api/professionals")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(post("/api/professionals").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }
}
