package br.com.clinica.service;

import br.com.clinica.dto.request.LoginRequestDTO;
import br.com.clinica.dto.response.LoginResponseDTO;
import br.com.clinica.dto.response.AuthenticatedUserResponseDTO;
import br.com.clinica.exception.ResourceNotFoundException;
import br.com.clinica.repository.ProfessionalRepository;
import br.com.clinica.repository.UserRepository;
import br.com.clinica.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ProfessionalRepository professionalRepository;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserRepository userRepository,
            ProfessionalRepository professionalRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.professionalRepository = professionalRepository;
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        var credentials = UsernamePasswordAuthenticationToken.unauthenticated(
                request.email(),
                request.password());

        var authentication = authenticationManager.authenticate(credentials);

        return jwtService.generateToken(authentication);
    }

    @Transactional(readOnly = true)
    public AuthenticatedUserResponseDTO me(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado."));
        var professionalId = professionalRepository.findByUser_Id(user.getId())
                .map(professional -> professional.getId())
                .orElse(null);
        return new AuthenticatedUserResponseDTO(
                user.getId(), user.getName(), user.getEmail(), user.getProfile(), professionalId);
    }
}
