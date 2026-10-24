package br.com.clinica.security;

import br.com.clinica.repository.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CurrentUserAccess {
    private final ProfessionalRepository professionals;

    public Role role() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .map(this::parseRole)
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Perfil não autorizado."));
    }

    public UUID professionalId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }
        return professionals.findByUser_Email(authentication.getName())
                .map(professional -> professional.getId())
                .orElseThrow(() -> new AccessDeniedException("Usuário não possui Professional vinculado."));
    }

    public void requireOwnProfessional(UUID professionalId) {
        if (!professionalId().equals(professionalId)) {
            throw new AccessDeniedException("Recurso pertence a outro profissional.");
        }
    }

    private Role parseRole(String value) {
        try {
            return Role.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Perfil não autorizado.");
        }
    }
}
