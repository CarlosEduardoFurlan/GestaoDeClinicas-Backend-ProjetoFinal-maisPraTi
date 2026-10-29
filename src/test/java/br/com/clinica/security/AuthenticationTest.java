package br.com.clinica.security;

import br.com.clinica.config.AuthenticationConfig;
import br.com.clinica.config.PasswordConfig;
import br.com.clinica.entity.User;
import br.com.clinica.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = new PasswordConfig().passwordEncoder();
    private final CustomUserDetailsService details = new CustomUserDetailsService(users);
    private final AuthenticationManager manager =
            new AuthenticationConfig().authenticationManager(details, encoder);
    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setEmail("usuario@example.com");
        user.setPassword(encoder.encode("senha-exclusiva-do-teste"));
        user.setProfile("ADMIN");
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void loadsApprovedRolesWithoutChangingStoredHash(Role role) {
        user.setProfile(role.name());
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        var loaded = details.loadUserByUsername(user.getEmail());
        assertEquals(user.getEmail(), loaded.getUsername());
        assertEquals(user.getPassword(), loaded.getPassword());
        assertEquals(List.of("ROLE_" + role.name()),
                loaded.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    }

    @Test
    void rejectsUnknownEmail() {
        when(users.findByEmail("ausente@example.com")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class,
                () -> details.loadUserByUsername("ausente@example.com"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ROOT", "admin", "ROLE_ADMIN", " ADMIN "})
    void rejectsUnapprovedProfiles(String profile) {
        user.setProfile(profile);
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        assertThrows(UsernameNotFoundException.class,
                () -> details.loadUserByUsername(user.getEmail()));
    }

    @Test
    void authenticatesWithRealBCryptAndErasesCredentials() {
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        var result = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                user.getEmail(), "senha-exclusiva-do-teste"));
        assertTrue(result.isAuthenticated());
        assertEquals(user.getEmail(), result.getName());
        assertNull(result.getCredentials());
        assertTrue(encoder.matches("senha-exclusiva-do-teste", user.getPassword()));
        assertNotEquals("senha-exclusiva-do-teste", user.getPassword());
    }

    @Test
    void rejectsIncorrectPasswordWithRealBCrypt() {
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        assertThrows(BadCredentialsException.class, () -> manager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(user.getEmail(), "incorreta")));
    }

    @Test
    void hidesWhetherEmailExists() {
        when(users.findByEmail("ausente@example.com")).thenReturn(Optional.empty());
        assertThrows(BadCredentialsException.class, () -> manager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("ausente@example.com", "incorreta")));
    }

    @Test
    void hashesSamePasswordWithDifferentSalts() {
        String anotherHash = encoder.encode("senha-exclusiva-do-teste");
        assertNotEquals(user.getPassword(), anotherHash);
        assertTrue(encoder.matches("senha-exclusiva-do-teste", anotherHash));
        assertFalse(encoder.matches("incorreta", anotherHash));
    }
}
