package br.com.clinica.service;

import br.com.clinica.dto.request.LoginRequestDTO;
import br.com.clinica.dto.response.LoginResponseDTO;
import br.com.clinica.security.JwtService;
import br.com.clinica.repository.UserRepository;
import br.com.clinica.repository.ProfessionalRepository;
import br.com.clinica.entity.User;
import br.com.clinica.entity.Professional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;
    @Mock private UserRepository userRepository;
    @Mock private ProfessionalRepository professionalRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void doesNotGenerateTokenWhenAuthenticationFails() {
        var request = new LoginRequestDTO(
                "usuario@example.com", "senha-incorreta");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException(
                        "Credenciais inválidas"));

        assertThrows(BadCredentialsException.class,
                () -> authService.login(request));

        verifyNoInteractions(jwtService);
    }

    @Test
    void generatesTokenAfterSuccessfulAuthentication() {
        var request = new LoginRequestDTO(
                "usuario@example.com", "senha-de-teste");

        var authenticated = UsernamePasswordAuthenticationToken.authenticated(
                request.email(), null, List.of());

        var expectedResponse = new LoginResponseDTO(
                "token-de-teste", "Bearer", 900);

        when(authenticationManager.authenticate(argThat(credentials ->
                request.email().equals(credentials.getPrincipal())
                        && request.password().equals(credentials.getCredentials())
                        && !credentials.isAuthenticated())))
                .thenReturn(authenticated);

        when(jwtService.generateToken(authenticated))
                .thenReturn(expectedResponse);

        var response = authService.login(request);

        assertSame(expectedResponse, response);
        verify(jwtService).generateToken(authenticated);
    }

    @Test
    void returnsAuthenticatedUserWithLinkedProfessional() {
        var user = new User(); user.setId(UUID.randomUUID()); user.setName("Ana");
        user.setEmail("ana@example.com"); user.setProfile("PROFESSIONAL");
        var professional = new Professional(); professional.setId(UUID.randomUUID());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(professionalRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(professional));
        var response = authService.me(user.getEmail());
        assertSame(user.getId(), response.userId());
        assertSame(professional.getId(), response.professionalId());
    }

    @Test
    void returnsNullProfessionalForAdministrativeUser() {
        var user = new User(); user.setId(UUID.randomUUID()); user.setName("Admin");
        user.setEmail("admin@example.com"); user.setProfile("ADMIN");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        var response = authService.me(user.getEmail());
        assertSame(user.getId(), response.userId());
        org.junit.jupiter.api.Assertions.assertNull(response.professionalId());
    }
}
