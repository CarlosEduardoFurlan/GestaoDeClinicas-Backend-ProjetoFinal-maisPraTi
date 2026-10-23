package br.com.clinica.security;

import br.com.clinica.entity.Professional;
import br.com.clinica.repository.ProfessionalRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrentUserAccessTest {
    private final ProfessionalRepository professionals = mock(ProfessionalRepository.class);
    private final CurrentUserAccess access = new CurrentUserAccess(professionals);

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void resolvesProfessionalFromJwtSubjectEmail() {
        authenticate("professional@example.invalid", "ROLE_PROFESSIONAL");
        var professional = new Professional(); professional.setId(UUID.randomUUID());
        when(professionals.findByUser_Email("professional@example.invalid"))
                .thenReturn(Optional.of(professional));
        assertEquals(Role.PROFESSIONAL, access.role());
        assertEquals(professional.getId(), access.professionalId());
    }

    @Test void deniesProfessionalWithoutLinkedProfessional() {
        authenticate("orphan@example.invalid", "ROLE_PROFESSIONAL");
        assertThrows(AccessDeniedException.class, access::professionalId);
    }

    @Test void deniesDifferentProfessionalAndInvalidRole() {
        authenticate("professional@example.invalid", "ROLE_PROFESSIONAL");
        var professional = new Professional(); professional.setId(UUID.randomUUID());
        when(professionals.findByUser_Email("professional@example.invalid"))
                .thenReturn(Optional.of(professional));
        assertThrows(AccessDeniedException.class, () -> access.requireOwnProfessional(UUID.randomUUID()));
        authenticate("invalid@example.invalid", "ROLE_PATIENT");
        assertThrows(AccessDeniedException.class, access::role);
    }

    private static void authenticate(String email, String authority) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(email, "n/a",
                        List.of(new SimpleGrantedAuthority(authority))));
    }
}
