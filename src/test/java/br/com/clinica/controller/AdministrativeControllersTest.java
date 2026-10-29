package br.com.clinica.controller;

import br.com.clinica.dto.request.*;
import br.com.clinica.dto.response.*;
import br.com.clinica.exception.*;
import br.com.clinica.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Contrato MVC isolado. SecurityHttpTest verifica os filtros reais e a negação das mesmas rotas.
class AdministrativeControllersTest {
    private final UserService users = mock(UserService.class);
    private final SpecialtyService specialties = mock(SpecialtyService.class);
    private final ProfessionalService professionals = mock(ProfessionalService.class);
    private final UUID id = UUID.randomUUID();
    private MockMvc mvc;
    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new UserController(users), new SpecialtyController(specialties),
                new ProfessionalController(professionals)).setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    private UserResponseDTO user() { return new UserResponseDTO(id,"Ana","ana@example.com","ADMIN",null); }
    private SpecialtyResponseDTO specialty() { return new SpecialtyResponseDTO(id,"Clínica","Descrição",null); }

    @Test void userCreationReturns201WithoutPassword() throws Exception {
        when(users.create(any())).thenReturn(user());
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Ana","email":"ana@example.com","password":"test-password","profile":"ADMIN"}
                """)).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.password").doesNotExist());
        verify(users).create(new UserRequestDTO("Ana","ana@example.com","test-password","ADMIN"));
    }
    @Test void userQueriesReturnDto() throws Exception {
        when(users.findAll()).thenReturn(List.of(user())); when(users.findById(id)).thenReturn(user());
        mvc.perform(get("/api/users")).andExpect(status().isOk()).andExpect(jsonPath("$[0].email").value("ana@example.com"));
        mvc.perform(get("/api/users/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist());
    }
    @Test void userUpdateUsesSeparateContract() throws Exception {
        when(users.update(eq(id),any())).thenReturn(user());
        mvc.perform(put("/api/users/"+id).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Ana","password":"new-password","profile":"ADMIN"}
                """)).andExpect(status().isOk());
        verify(users).update(id,new UserUpdateRequestDTO("Ana","new-password","ADMIN"));
    }
    @Test void deletesDelegateAndReturn204() throws Exception {
        mvc.perform(delete("/api/users/"+id)).andExpect(status().isNoContent()); verify(users).delete(id);
        mvc.perform(delete("/api/specialties/"+id)).andExpect(status().isNoContent()); verify(specialties).delete(id);
        mvc.perform(delete("/api/professionals/"+id)).andExpect(status().isNoContent()); verify(professionals).delete(id);
    }
    @Test void specialtyCrudReturnsExpectedStatuses() throws Exception {
        when(specialties.create(any())).thenReturn(specialty()); when(specialties.update(eq(id),any())).thenReturn(specialty());
        when(specialties.findAll()).thenReturn(List.of(specialty())); when(specialties.findById(id)).thenReturn(specialty());
        mvc.perform(post("/api/specialties").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Clínica\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id.toString()));
        mvc.perform(get("/api/specialties")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Clínica"));
        mvc.perform(get("/api/specialties/"+id)).andExpect(status().isOk());
        mvc.perform(put("/api/specialties/"+id).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Clínica\"}"))
                .andExpect(status().isOk());
    }
    @Test void invalidRequestsNeverReachServices() throws Exception {
        for(String path:List.of("users","specialties")) {
            mvc.perform(post("/api/"+path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.details").isArray());
            mvc.perform(get("/api/"+path+"/invalid")).andExpect(status().isBadRequest());
        }
        mvc.perform(put("/api/professionals/"+id).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(users,specialties,professionals);
    }
    @Test void invalidProfileIs400() throws Exception {
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Ana","email":"ana@example.com","password":"test-password","profile":"USER"}
                """)).andExpect(status().isBadRequest()); verifyNoInteractions(users);
    }
    @Test void errorsKeepSharedFormat() throws Exception {
        when(users.findById(id)).thenThrow(new ResourceNotFoundException("Usuário não encontrado"));
        mvc.perform(get("/api/users/"+id)).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        when(specialties.create(any())).thenThrow(new BusinessException("Duplicado"));
        mvc.perform(post("/api/specialties").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Clínica\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Duplicado"));
        doThrow(new DataIntegrityViolationException("FK")).when(specialties).delete(id);
        mvc.perform(delete("/api/specialties/"+id)).andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
    }
    @Test void professionalUpdateDoesNotRequireAuthenticationFields() throws Exception {
        when(professionals.update(eq(id),any())).thenReturn(new ProfessionalResponseDTO(id,"Ana","CRM-1","123","123","ana@example.com",
                UUID.randomUUID(),"Clínica",new AddressResponseDTO("Rua A",1,null,"Centro","Cuiaba","MT","78000-000"),"Cuiaba",UUID.randomUUID()));
        mvc.perform(put("/api/professionals/"+id).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Ana","register":"CRM-1","telephone":"123","cellPhone":"123","email":"ana@example.com",
                 "specialtyId":"%s","address":{"address":"Rua A","number":1,"district":"Centro","city":"Cuiaba","uf":"MT","zipCode":"78000-000"}}
                """.formatted(UUID.randomUUID()))).andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist());
    }
}
