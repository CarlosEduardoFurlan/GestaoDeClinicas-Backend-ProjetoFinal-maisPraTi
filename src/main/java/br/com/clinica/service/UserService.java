package br.com.clinica.service;


import br.com.clinica.dto.request.UserRequestDTO;
import br.com.clinica.dto.response.UserResponseDTO;
import br.com.clinica.entity.User;
import br.com.clinica.exception.BusinessException;
import br.com.clinica.exception.ResourceNotFoundException;
import br.com.clinica.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Conjunto de perfis aceitos/aprovados no sistema (Regra 5)
    private static final Set<String> APPROVED_PROFILES = Set.of("ADMIN", "RECEPTIONIST", "PROFESSIONAL");

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponseDTO create(UserRequestDTO dto) {

        // 1. validar profile
        validateProfile(dto.profile());

        // 2. Verificar duplicidade de e-mail
        if (userRepository.existsByEmail(dto.email())) {
            throw new BusinessException("E-mail já cadastrado no sistema.");
        }

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName(dto.name());
        user.setEmail(dto.email());

        // 3. Armazenar a senha criptografada usando o PasswordEncoder/BCrypt
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setProfile(dto.profile().toUpperCase());
        user.setCreated(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));

        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO findById(UUID id) {
        // 6. Reutilizar ResourceNotFoundException para usuário inexistente
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));
        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAll() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponseDTO update(UUID id, UserRequestDTO request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));

        validateProfile(request.profile());

        // Verifica se o e-mail mudou e se o novo já pertence a outra pessoa
        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new BusinessException("O e-mail informado já está em uso por outro usuário.");
        }

        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setProfile(request.profile().toUpperCase());

        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    @Transactional
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuário não encontrado com o ID: " + id);
        }
        userRepository.deleteById(id);
    }

    // Regra 5: Validação manual de regras de negócio independente da camada de Controller
    private void validateProfile(String profile) {
        if (profile == null || !APPROVED_PROFILES.contains(profile.toUpperCase())) {
            throw new BusinessException("Perfil inválido ou não aprovado pelo sistema.");
        }
    }

    // Regra 2: Converte a entidade mapeando apenas campos permitidos (sem password/hash)
    private UserResponseDTO mapToResponse(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getProfile(),
                user.getCreated()
        );
    }
}
