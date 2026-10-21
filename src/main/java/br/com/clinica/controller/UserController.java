package br.com.clinica.controller;

import br.com.clinica.dto.request.UserRequestDTO;
import br.com.clinica.dto.request.UserUpdateRequestDTO;
import br.com.clinica.dto.response.UserResponseDTO;
import br.com.clinica.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService users;

    @GetMapping public ResponseEntity<List<UserResponseDTO>> findAll() {
        return ResponseEntity.ok(users.findAll());
    }
    @GetMapping("/{id}") public ResponseEntity<UserResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(users.findById(id));
    }
    @PostMapping public ResponseEntity<UserResponseDTO> create(@RequestBody @Valid UserRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(users.create(request));
    }
    @PutMapping("/{id}") public ResponseEntity<UserResponseDTO> update(
            @PathVariable UUID id, @RequestBody @Valid UserUpdateRequestDTO request) {
        return ResponseEntity.ok(users.update(id, request));
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable UUID id) {
        users.delete(id);
        return ResponseEntity.noContent().build();
    }
}

