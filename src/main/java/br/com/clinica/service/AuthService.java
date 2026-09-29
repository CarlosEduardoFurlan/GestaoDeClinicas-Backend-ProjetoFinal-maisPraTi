package br.com.clinica.service;

import br.com.clinica.dto.request.LoginRequestDTO;
import br.com.clinica.dto.response.LoginResponseDTO;
import br.com.clinica.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        var credentials = UsernamePasswordAuthenticationToken.unauthenticated(
                request.email(),
                request.password());

        var authentication = authenticationManager.authenticate(credentials);

        return jwtService.generateToken(authentication);
    }
}