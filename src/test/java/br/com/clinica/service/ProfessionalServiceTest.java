package br.com.clinica.service;

import br.com.clinica.dto.request.*;
import br.com.clinica.entity.*;
import br.com.clinica.exception.*;
import br.com.clinica.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfessionalServiceTest {
    @Mock ProfessionalRepository professionals;
    @Mock UserRepository users;
    @Mock AddressRepository addresses;
    @Mock SpecialtyRepository specialties;
    @Mock PasswordEncoder encoder;
    @InjectMocks ProfessionalService service;
    ProfessionalRequestDTO request=new ProfessionalRequestDTO("Ana","CRM-123","6533333333","65999999999",
            "ana@example.com",UUID.randomUUID(),new AddressRequestDTO("Rua A",1,null,"Centro","Cuiaba","MT","78000-000"),
            new UserRequestDTO("Ana","ana@example.com","test-password","PROFESSIONAL"));
    @Test void rejectsDuplicateRegisterBeforeCreatingUser() {
        when(professionals.existsByRegister(request.register())).thenReturn(true);
        assertThrows(BusinessException.class,()->service.create(request)); verifyNoInteractions(users,addresses,encoder);
    }
    @Test void rejectsDuplicateUserEmail() {
        when(users.existsByEmail(request.user().email())).thenReturn(true);
        assertThrows(BusinessException.class,()->service.create(request)); verify(users,never()).save(any());
    }
    @Test void rejectsMissingSpecialtyBeforeCreatingUser() {
        assertThrows(ResourceNotFoundException.class,()->service.create(request)); verify(users,never()).save(any()); verifyNoInteractions(addresses,encoder);
    }
    @Test void missingProfessionalIsNotFound() { assertThrows(ResourceNotFoundException.class,()->service.findById(UUID.randomUUID())); }
    @Test void mapsProfessionalWithoutExposingPassword() {
        var p=new Professional(); p.setId(UUID.randomUUID()); p.setName("Ana");
        var user=new User(); user.setId(UUID.randomUUID()); user.setPassword("secret-hash"); p.setUser(user);
        when(professionals.findById(p.getId())).thenReturn(Optional.of(p));
        assertEquals(p.getId(),service.findById(p.getId()).id());
    }
}
