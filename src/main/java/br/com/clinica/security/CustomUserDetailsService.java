package br.com.clinica.security;

import br.com.clinica.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Usuário não encontrado"));

        Role role = Arrays.stream(Role.values())
                .filter(candidate -> candidate.name().equals(user.getProfile()))
                .findFirst()
                .orElseThrow(() ->
                        new UsernameNotFoundException("Usuário com perfil inválido"));

        return User.withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(role.name())
                .build();
    }
}