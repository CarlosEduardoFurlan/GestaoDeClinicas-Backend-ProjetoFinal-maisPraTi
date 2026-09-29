package br.com.clinica.security;

import br.com.clinica.config.*;
import br.com.clinica.controller.*;
import br.com.clinica.entity.User;
import br.com.clinica.dto.request.PatientCreateRequest;
import br.com.clinica.dto.request.PatientUpdateRequest;
import br.com.clinica.dto.response.PatientResponse;
import br.com.clinica.exception.GlobalExceptionHandler;
import br.com.clinica.repository.UserRepository;
import br.com.clinica.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Filtros, BCrypt e JWT reais. Apenas persistência e serviços de negócio
 * são simulados. Não carrega application.properties nem conecta ao banco.
 */
@WebMvcTest(controllers = {AuthController.class, ProfessionalController.class, PatientController.class},
        properties = "spring.config.location=optional:classpath:/security-test-isolated.properties")
@Import({SecurityConfig.class, JwtConfig.class, PasswordConfig.class, AuthenticationConfig.class,
        CustomUserDetailsService.class, JwtService.class, AuthService.class,
        ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class, GlobalExceptionHandler.class})
class SecurityHttpTest {
    private static final String SECRET = JwtTestSupport.randomSecret();
    private static final String ISSUER = "clinica-security-http-test";
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("security.jwt.secret", () -> SECRET);
        registry.add("security.jwt.issuer", () -> ISSUER);
        registry.add("security.jwt.expiration-seconds", () -> 900);
    }

    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtEncoder encoder;
    @Autowired JwtDecoder decoder;
    @MockitoBean UserRepository users;
    @MockitoBean ProfessionalService professionals;
    @MockitoBean PatientService patients;

    private User user(Role role) {
        var user = new User();
        user.setEmail("usuario@example.com");
        user.setProfile(role.name());
        user.setPassword(passwords.encode("senha-de-teste"));
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        return user;
    }

    private ResultActions login(String body) throws Exception {
        return mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String token(List<String> authorities) {
        Instant now = Instant.now();
        return JwtTestSupport.token(encoder, ISSUER, authorities,
                now, now.plusSeconds(900), now);
    }

    private ResultActions protectedPost(String token) throws Exception {
        return mvc.perform(post("/api/professionals")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{}"));
    }

    private void unauthorized(ResultActions result, String path) throws Exception {
        result.andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Autenticação necessária ou token inválido"))
                .andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
        verifyNoInteractions(professionals, patients);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void loginReturnsRealTokenWithoutPasswordOrSession(Role role) throws Exception {
        User user = user(role);
        var result = login("""
                {"email":"usuario@example.com","password":"senha-de-teste"}
                """).andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(cookie().doesNotExist("JSESSIONID")).andReturn();
        var body = json.readTree(result.getResponse().getContentAsString());
        assertEquals(3, body.size());
        var jwt = decoder.decode(body.get("accessToken").asText());
        assertEquals(user.getEmail(), jwt.getSubject());
        assertEquals(List.of("ROLE_" + role.name()), jwt.getClaimAsStringList("authorities")
                .stream().filter(authority -> authority.startsWith("ROLE_")).toList());
        assertFalse(jwt.getClaims().containsKey("password"));
        assertNull(result.getRequest().getSession(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{", "{\"email\":\"invalido\",\"password\":\"abc\"}",
            "{\"email\":\"usuario@example.com\",\"password\":\" \"}"})
    void rejectsInvalidLoginBodyBeforeRepository(String body) throws Exception {
        login(body).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
        verifyNoInteractions(users);
    }

    @Test
    void wrongPasswordAndMissingUserReturnSamePublicError() throws Exception {
        user(Role.ADMIN);
        String wrong = login("""
                {"email":"usuario@example.com","password":"errada"}
                """).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        when(users.findByEmail("ausente@example.com")).thenReturn(Optional.empty());
        String absent = login("""
                {"email":"ausente@example.com","password":"errada"}
                """).andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertEquals("E-mail ou senha inválidos", json.readTree(wrong).get("message").asText());
        assertEquals(json.readTree(wrong).get("message"), json.readTree(absent).get("message"));
    }

    @Test
    void invalidStoredProfileCannotLogin() throws Exception {
        User user = user(Role.ADMIN);
        user.setProfile("ROOT");
        login("""
                {"email":"usuario@example.com","password":"senha-de-teste"}
                """).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void missingTokenUsesJson401() throws Exception {
        unauthorized(mvc.perform(post("/api/professionals")
                .contentType(MediaType.APPLICATION_JSON).content("{}")), "/api/professionals");
    }

    @Test
    void malformedTokenUsesJson401() throws Exception {
        unauthorized(protectedPost("token-invalido"), "/api/professionals");
    }

    @Test
    void expiredTokenUsesJson401() throws Exception {
        Instant now = Instant.now();
        String expired = JwtTestSupport.token(encoder, ISSUER, List.of("ROLE_ADMIN"),
                now.minusSeconds(1800), now.minusSeconds(120), now.minusSeconds(1800));
        unauthorized(protectedPost(expired), "/api/professionals");
    }

    @Test
    void foreignSignatureUsesJson401() throws Exception {
        Instant now = Instant.now();
        var foreignEncoder = new JwtConfig().jwtEncoder(JwtTestSupport.randomKey());
        String foreign = JwtTestSupport.token(foreignEncoder, ISSUER, List.of("ROLE_ADMIN"),
                now, now.plusSeconds(900), now);
        unauthorized(protectedPost(foreign), "/api/professionals");
    }

    @Test
    void adminReachesRequestValidation() throws Exception {
        protectedPost(token(List.of("ROLE_ADMIN"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados inválidos"))
                .andExpect(jsonPath("$.details").isArray());
        verifyNoInteractions(professionals);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"RECEPTIONIST", "PROFESSIONAL"})
    void otherRolesCannotCreateProfessionals(Role role) throws Exception {
        protectedPost(token(List.of("ROLE_" + role.name())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Você não tem permissão para acessar este recurso"))
                .andExpect(jsonPath("$.path").value("/api/professionals"))
                .andExpect(jsonPath("$.timestamp").exists());
        verifyNoInteractions(professionals);
    }

    @Test
    void tokenWithoutAuthoritiesCannotCreateProfessionals() throws Exception {
        protectedPost(token(List.of())).andExpect(status().isForbidden());
        verifyNoInteractions(professionals);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/patients/00000000-0000-0000-0000-000000000001/history", "/api/professionals/00000000-0000-0000-0000-000000000001/history"})
    void unspecifiedRoutesRemainDeniedEvenForAdmin(String path) throws Exception {
        mvc.perform(get(path).header("Authorization", "Bearer " + token(List.of("ROLE_ADMIN"))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.path").value(path));
        verifyNoInteractions(professionals, patients);
    }

    @Test
    void loginDoesNotCreateSessionUsableOnNextRequest() throws Exception {
        user(Role.ADMIN);
        login("""
                {"email":"usuario@example.com","password":"senha-de-teste"}
                """).andExpect(status().isOk()).andExpect(cookie().doesNotExist("JSESSIONID"));
        unauthorized(mvc.perform(get("/api/patients")), "/api/patients");
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "RECEPTIONIST"})
    void approvedRolesCanCreatePatients(Role role) throws Exception {
        UUID addressId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        var request = new PatientCreateRequest("Maria", "12345678901",
                LocalDate.of(1990, 5, 20), null, "65999999999",
                "maria@example.com", "F", addressId);
        when(patients.create(request)).thenReturn(new PatientResponse(patientId,
                request.name(), request.cpf(), request.dateBirth(), request.telephone(),
                request.cellPhone(), request.email(), request.gender(), addressId));

        mvc.perform(post("/api/patients")
                .header("Authorization", "Bearer " + token(List.of("ROLE_" + role.name())))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(patientId.toString()))
                .andExpect(jsonPath("$.name").value("Maria"));
        verify(patients).create(request);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "RECEPTIONIST"})
    void approvedRolesStillRequireValidPatientBody(Role role) throws Exception {
        mvc.perform(post("/api/patients")
                .header("Authorization", "Bearer " + token(List.of("ROLE_" + role.name())))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados inválidos"));
        verifyNoInteractions(patients);
    }

    @Test
    void professionalCannotCreatePatients() throws Exception {
        mvc.perform(post("/api/patients")
                .header("Authorization", "Bearer " + token(List.of("ROLE_PROFESSIONAL")))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value("/api/patients"));
        verifyNoInteractions(patients);
    }

    @Test
    void patientCreationRequiresAuthentication() throws Exception {
        unauthorized(mvc.perform(post("/api/patients")
                .contentType(MediaType.APPLICATION_JSON).content("{}")), "/api/patients");
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "RECEPTIONIST"})
    void approvedRolesCanListPatients(Role role) throws Exception {
        UUID id = UUID.randomUUID();
        var patient = new PatientResponse(id, "Maria", "12345678901",
                LocalDate.of(1990, 5, 20), null, "65999999999",
                "maria@example.com", "F", UUID.randomUUID());
        when(patients.findAll()).thenReturn(List.of(patient));
        mvc.perform(get("/api/patients")
                .header("Authorization", "Bearer " + token(List.of("ROLE_" + role.name()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].name").value("Maria"));
        verify(patients).findAll();
    }

    @Test
    void professionalCannotListPatients() throws Exception {
        mvc.perform(get("/api/patients")
                .header("Authorization", "Bearer " + token(List.of("ROLE_PROFESSIONAL"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value("/api/patients"));
        verifyNoInteractions(patients);
    }

    @Test
    void patientListingRequiresAuthentication() throws Exception {
        unauthorized(mvc.perform(get("/api/patients")), "/api/patients");
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "RECEPTIONIST"})
    void approvedRolesCanFindPatientById(Role role) throws Exception {
        UUID id = UUID.randomUUID();
        var patient = new PatientResponse(id, "Maria", "12345678901",
                LocalDate.of(1990, 5, 20), null, "65999999999",
                "maria@example.com", "F", UUID.randomUUID());
        when(patients.findById(id)).thenReturn(patient);
        mvc.perform(get("/api/patients/{id}", id)
                .header("Authorization", "Bearer " + token(List.of("ROLE_" + role.name()))))
                .andExpect(status().isOk())
                .andExpect(content().json(json.writeValueAsString(patient)));
        verify(patients).findById(id);
    }

    @Test
    void professionalCannotFindPatientById() throws Exception {
        String path = "/api/patients/" + UUID.randomUUID();
        mvc.perform(get(path)
                .header("Authorization", "Bearer " + token(List.of("ROLE_PROFESSIONAL"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value(path));
        verifyNoInteractions(patients);
    }

    @Test
    void patientLookupRequiresAuthentication() throws Exception {
        String path = "/api/patients/" + UUID.randomUUID();
        unauthorized(mvc.perform(get(path)), path);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "RECEPTIONIST"})
    void missingPatientReturns404ForApprovedRoles(Role role) throws Exception {
        UUID id = UUID.randomUUID();
        when(patients.findById(id)).thenThrow(
                new br.com.clinica.exception.ResourceNotFoundException("Paciente não encontrado: " + id));
        mvc.perform(get("/api/patients/{id}", id)
                .header("Authorization", "Bearer " + token(List.of("ROLE_" + role.name()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        verify(patients).findById(id);
    }

    @Test
    void invalidPatientIdReturns400() throws Exception {
        mvc.perform(get("/api/patients/invalid")
                .header("Authorization", "Bearer " + token(List.of("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(patients);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PATCH", "DELETE"})
    void patientMutationByIdRemainsDenied(String method) throws Exception {
        mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method),
                        "/api/patients/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + token(List.of("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(patients);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "RECEPTIONIST"})
    void approvedRolesCanUpdatePatients(Role role) throws Exception {
        UUID id = UUID.randomUUID();
        var body = new PatientUpdateRequest("Maria Atualizada", LocalDate.of(1990, 5, 20),
                null, "65999999999", "maria@example.com", "F", UUID.randomUUID());
        var response = new PatientResponse(id, body.name(), "12345678901", body.dateBirth(),
                body.telephone(), body.cellPhone(), body.email(), body.gender(), body.addressId());
        when(patients.update(id, body)).thenReturn(response);
        mvc.perform(put("/api/patients/{id}", id)
                .header("Authorization", "Bearer " + token(List.of("ROLE_" + role.name())))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(content().json(json.writeValueAsString(response)));
        verify(patients).update(id, body);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "RECEPTIONIST"})
    void patientUpdateRequiresValidBody(Role role) throws Exception {
        mvc.perform(put("/api/patients/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer " + token(List.of("ROLE_" + role.name())))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados inválidos"));
        verifyNoInteractions(patients);
    }

    @Test
    void professionalCannotUpdatePatients() throws Exception {
        mvc.perform(put("/api/patients/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer " + token(List.of("ROLE_PROFESSIONAL")))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verifyNoInteractions(patients);
    }

    @Test
    void patientUpdateRequiresAuthentication() throws Exception {
        String path = "/api/patients/" + UUID.randomUUID();
        unauthorized(mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON)
                .content("{}")), path);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "RECEPTIONIST"})
    void approvedRolesCanListProfessionals(Role role) throws Exception {
        var professional = new br.com.clinica.dto.response.ProfessionalResponseDTO(
                UUID.randomUUID(), "Ana", "CRM-12345", null, "65999999999",
                "ana@example.com", "Cardiologia", "Cuiabá", UUID.randomUUID());
        when(professionals.findAll()).thenReturn(List.of(professional));
        mvc.perform(get("/api/professionals")
                .header("Authorization", "Bearer " + token(List.of("ROLE_" + role.name()))))
                .andExpect(status().isOk())
                .andExpect(content().json(json.writeValueAsString(List.of(professional))));
        verify(professionals).findAll();
    }

    @Test
    void professionalCannotListProfessionals() throws Exception {
        mvc.perform(get("/api/professionals")
                .header("Authorization", "Bearer " + token(List.of("ROLE_PROFESSIONAL"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value("/api/professionals"));
        verifyNoInteractions(professionals);
    }

    @Test
    void professionalListingRequiresAuthentication() throws Exception {
        unauthorized(mvc.perform(get("/api/professionals")), "/api/professionals");
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "RECEPTIONIST"})
    void approvedRolesCanFindProfessionalById(Role role) throws Exception {
        UUID id = UUID.randomUUID();
        var professional = new br.com.clinica.dto.response.ProfessionalResponseDTO(
                id, "Ana", "CRM-12345", null, "65999999999",
                "ana@example.com", "Cardiologia", "Cuiabá", UUID.randomUUID());
        when(professionals.findById(id)).thenReturn(professional);
        mvc.perform(get("/api/professionals/{id}", id)
                .header("Authorization", "Bearer " + token(List.of("ROLE_" + role.name()))))
                .andExpect(status().isOk())
                .andExpect(content().json(json.writeValueAsString(professional)));
        verify(professionals).findById(id);
    }

    @Test
    void professionalCannotFindProfessionalById() throws Exception {
        String path = "/api/professionals/" + UUID.randomUUID();
        mvc.perform(get(path)
                .header("Authorization", "Bearer " + token(List.of("ROLE_PROFESSIONAL"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value(path));
        verifyNoInteractions(professionals);
    }

    @Test
    void professionalLookupRequiresAuthentication() throws Exception {
        String path = "/api/professionals/" + UUID.randomUUID();
        unauthorized(mvc.perform(get(path)), path);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "RECEPTIONIST"})
    void missingProfessionalReturns404ForApprovedRoles(Role role) throws Exception {
        UUID id = UUID.randomUUID();
        when(professionals.findById(id)).thenThrow(
                new br.com.clinica.exception.ResourceNotFoundException("Profissional não encontrado: " + id));
        mvc.perform(get("/api/professionals/{id}", id)
                .header("Authorization", "Bearer " + token(List.of("ROLE_" + role.name()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        verify(professionals).findById(id);
    }

    @Test
    void invalidProfessionalIdReturns400() throws Exception {
        mvc.perform(get("/api/professionals/invalid")
                .header("Authorization", "Bearer " + token(List.of("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(professionals);
    }

    private br.com.clinica.dto.request.ProfessionalRequestDTO professionalRequest(String profile) {
        return new br.com.clinica.dto.request.ProfessionalRequestDTO(
                "Ana", "CRM-12345", "6533333333", "65999999999", "ana@example.com",
                UUID.randomUUID(),
                new br.com.clinica.dto.request.AddressRequestDTO(
                        "Rua A", 1, null, "Centro", "Cuiaba", "MT", "78000-000"),
                new br.com.clinica.dto.request.UserRequestDTO(
                        "Ana", "ana@example.com", "test-password", profile));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @ValueSource(strings = {"ROOT", "admin", "ROLE_ADMIN", " ADMIN ", " "})
    void invalidRegistrationProfileIsRejectedBeforeService(String profile) throws Exception {
        mvc.perform(post("/api/professionals")
                .header("Authorization", "Bearer " + token(List.of("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(professionalRequest(profile))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados inválidos"))
                .andExpect(jsonPath("$.details").isNotEmpty());
        verifyNoInteractions(professionals, users);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void adminCanSubmitEachApprovedProfile(Role role) throws Exception {
        var request = professionalRequest(role.name());
        mvc.perform(post("/api/professionals")
                .header("Authorization", "Bearer " + token(List.of("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isCreated());
        verify(professionals).create(request);
    }
}
