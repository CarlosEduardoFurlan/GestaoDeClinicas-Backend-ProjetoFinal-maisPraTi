package br.com.clinica.service;

import br.com.clinica.dto.request.*;
import br.com.clinica.entity.*;
import br.com.clinica.exception.*;
import br.com.clinica.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsersProfessionalsRegressionTest {
    private final UserRepository users = mock(UserRepository.class);
    private final ProfessionalRepository professionals = mock(ProfessionalRepository.class);
    private final AddressRepository addresses = mock(AddressRepository.class);
    private final SpecialtyRepository specialties = mock(SpecialtyRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final UserService userService = new UserService(users, encoder);
    private final ProfessionalService professionalService = new ProfessionalService(
            professionals, users, addresses, specialties, encoder);

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPTIONIST", "PROFESSIONAL"})
    void createsApprovedProfilesWithRealBcrypt(String profile) {
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));
        var result = userService.create(new UserRequestDTO("Ana", "ana@example.com", "test-password", profile));
        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        assertTrue(encoder.matches("test-password", captor.getValue().getPassword()));
        assertEquals(profile, result.profile());
        assertTrue(java.util.Arrays.stream(result.getClass().getRecordComponents())
                .noneMatch(field -> field.getName().equals("password")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"USER", "PATIENT", "admin", "ROLE_ADMIN", " ADMIN", "PROFESSIONAL ", "ROOT"})
    void bothCreationPathsRejectInvalidProfilesBeforePersistence(String profile) {
        var request = new UserRequestDTO("Ana", "ana@example.com", "test-password", profile);
        assertThrows(BusinessException.class, () -> userService.create(request));
        assertThrows(BusinessException.class, () -> professionalService.create(
                new ProfessionalRequestDTO("Ana", "CRM-1", "123", "123", "a@example.com",
                        UUID.randomUUID(), addressRequest(), request)));
        verifyNoInteractions(users, professionals, addresses, specialties);
    }

    @Test void userUpdatePreservesLoginIdentityAndHashesPassword() {
        var user = new User(); user.setId(UUID.randomUUID()); user.setEmail("stable@example.com");
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);
        var result = userService.update(user.getId(), new UserUpdateRequestDTO("Novo nome", "new-password", "ADMIN"));
        assertEquals("stable@example.com", result.email());
        assertTrue(encoder.matches("new-password", user.getPassword()));
        assertEquals("Novo nome", result.name());
    }

    @Test void userUpdateRejectsInvalidProfile() {
        var user = new User(); var id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.of(user));
        assertThrows(BusinessException.class, () -> userService.update(id,
                new UserUpdateRequestDTO("Ana", "test-password", "USER")));
        verify(users, never()).save(any());
    }

    @Test void professionalUpdatePreservesAuthenticationAccount() {
        var professional = professional();
        var specialty = new Specialty(); specialty.setId(UUID.randomUUID());
        when(professionals.findById(professional.getId())).thenReturn(Optional.of(professional));
        when(specialties.findById(specialty.getId())).thenReturn(Optional.of(specialty));
        when(professionals.save(professional)).thenReturn(professional);
        var originalUser = professional.getUser();
        var result = professionalService.update(professional.getId(), updateRequest(specialty.getId(), "CRM-1"));
        assertSame(originalUser, professional.getUser());
        assertEquals(originalUser.getId(), result.userId());
        assertSame(specialty, professional.getSpecialty());
        verifyNoInteractions(users);
        verify(professionals, never()).existsByRegister(any());
    }

    @Test void professionalUpdateRejectsMissingSpecialty() {
        var p = professional();
        when(professionals.findById(p.getId())).thenReturn(Optional.of(p));
        assertThrows(ResourceNotFoundException.class, () -> professionalService.update(p.getId(), updateRequest(UUID.randomUUID(), "CRM-1")));
        verify(professionals, never()).save(any()); verifyNoInteractions(users, addresses);
    }

    @Test void professionalUpdateRejectsDuplicateRegister() {
        var p = professional(); var specialtyId = UUID.randomUUID();
        when(professionals.findById(p.getId())).thenReturn(Optional.of(p));
        when(specialties.findById(specialtyId)).thenReturn(Optional.of(new Specialty()));
        when(professionals.existsByRegister("CRM-2")).thenReturn(true);
        assertThrows(BusinessException.class, () -> professionalService.update(p.getId(), updateRequest(specialtyId, "CRM-2")));
        verify(professionals, never()).save(any()); verifyNoInteractions(users, addresses);
    }

    @Test void professionalDeleteDoesNotDeleteUserOrAddress() {
        var p = professional();
        when(professionals.findById(p.getId())).thenReturn(Optional.of(p));
        professionalService.delete(p.getId());
        verify(professionals).delete(p); verifyNoInteractions(users, addresses, specialties);
    }

    @Test void missingProfessionalCannotBeUpdatedOrDeleted() {
        var id = UUID.randomUUID();
        assertThrows(ResourceNotFoundException.class, () -> professionalService.update(id, updateRequest(UUID.randomUUID(), "CRM-1")));
        assertThrows(ResourceNotFoundException.class, () -> professionalService.delete(id));
    }

    private Professional professional() {
        var p = new Professional(); p.setId(UUID.randomUUID()); p.setRegister("CRM-1");
        var user = new User(); user.setId(UUID.randomUUID()); p.setUser(user); p.setAddress(new Address());
        return p;
    }
    private AddressRequestDTO addressRequest() {
        return new AddressRequestDTO("Rua A", 1, null, "Centro", "Cuiaba", "MT", "78000-000");
    }
    private ProfessionalUpdateRequestDTO updateRequest(UUID specialty, String register) {
        return new ProfessionalUpdateRequestDTO("Ana", register, "123", "123", "contato@example.com", specialty, addressRequest());
    }
}
