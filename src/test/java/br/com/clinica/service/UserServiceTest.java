package br.com.clinica.service;

import br.com.clinica.dto.request.UserRequestDTO;
import br.com.clinica.dto.response.UserResponseDTO;
import br.com.clinica.entity.User;
import br.com.clinica.exception.BusinessException;
import br.com.clinica.exception.ResourceNotFoundException;
import br.com.clinica.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Cenário: criação válida - salva senha com BCrypt e retorna resposta sem password")
    void deveSalvarComBcryptERetornarSemPassword() {
        UserRequestDTO request = new UserRequestDTO("João Silva", "joao@email.com", "senha123", "USER");

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hash_bcrypt_123");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User userSalvo = invocation.getArgument(0);
            userSalvo.setId(UUID.randomUUID());
            return userSalvo;
        });

        UserResponseDTO response = userService.create(request);

        assertNotNull(response);
        assertNotNull(response.id());
        assertEquals("João Silva", response.name());
        assertEquals("joao@email.com", response.email());
        assertEquals("USER", response.profile());
        // O DTO de resposta sequer possui a propriedade password
        verify(passwordEncoder, times(1)).encode("senha123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Cenário: e-mail duplicado - falha com erro de negócio")
    void deveFalharQuandoEmailDuplicado() {
        UserRequestDTO request = new UserRequestDTO("João Silva", "duplicado@email.com", "senha123", "USER");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.create(request));

        assertEquals("E-mail já cadastrado no sistema.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Cenário: profile inválido fora do HTTP - service não persiste valor não aprovado")
    void deveFalharQuandoProfileInvalidoForaDoHttp() {
        // Ignorando a validação sintática do Bean Validation (@Pattern) simulando execução direto no Service
        UserRequestDTO request = new UserRequestDTO("João Silva", "joao@email.com", "senha123", "INVALID_PROFILE");

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.create(request));

        assertEquals("Perfil inválido ou não aprovado pelo sistema.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Cenário: inexistente - ResourceNotFoundException")
    void deveLancarResourceNotFoundExceptionQuandoUsuarioInexistente() {
        UUID idInexistente = UUID.randomUUID();
        when(userRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.findById(idInexistente));
    }
}
