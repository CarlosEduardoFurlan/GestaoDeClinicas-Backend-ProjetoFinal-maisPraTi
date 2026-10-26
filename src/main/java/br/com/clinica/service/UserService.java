package br.com.clinica.service;


import br.com.clinica.dto.request.UserRequestDTO;
import br.com.clinica.dto.request.UserUpdateRequestDTO;
import br.com.clinica.dto.response.UserResponseDTO;
import br.com.clinica.entity.User;
import br.com.clinica.exception.BusinessException;
import br.com.clinica.exception.ResourceNotFoundException;
import br.com.clinica.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponseDTO create(UserRequestDTO dto) {

        // 1. validar profile
        ProfileValidation.validate(dto.profile());

        // 2. Verificar duplicidade de e-mail
        if (userRepository.existsByEmail(dto.email())) {
            throw new BusinessException("E-mail já cadastrado no sistema.");
        }

        User user = new User();
        user.setName(dto.name());
        user.setEmail(dto.email());

        // 3. Armazenar a senha criptografada usando o PasswordEncoder/BCrypt
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setProfile(dto.profile());

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
    public UserResponseDTO update(UUID id, UserUpdateRequestDTO request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));

        ProfileValidation.validate(request.profile());

        user.setName(request.name());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setProfile(request.profile());

        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    @Transactional
    public void delete(UUID id) {
        if (userRepository.deleteDirectlyById(id) == 0) {
            throw new ResourceNotFoundException("Usuário não encontrado com o ID: " + id);
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
