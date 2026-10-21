package br.com.clinica.controller;

import br.com.clinica.dto.request.SpecialtyRequestDTO;
import br.com.clinica.dto.response.SpecialtyResponseDTO;
import br.com.clinica.service.SpecialtyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/specialties")
@RequiredArgsConstructor
public class SpecialtyController {
    private final SpecialtyService specialties;

    @GetMapping public ResponseEntity<List<SpecialtyResponseDTO>> findAll() {
        return ResponseEntity.ok(specialties.findAll());
    }
    @GetMapping("/{id}") public ResponseEntity<SpecialtyResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(specialties.findById(id));
    }
    @PostMapping public ResponseEntity<SpecialtyResponseDTO> create(@RequestBody @Valid SpecialtyRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(specialties.create(request));
    }
    @PutMapping("/{id}") public ResponseEntity<SpecialtyResponseDTO> update(
            @PathVariable UUID id, @RequestBody @Valid SpecialtyRequestDTO request) {
        return ResponseEntity.ok(specialties.update(id, request));
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable UUID id) {
        specialties.delete(id);
        return ResponseEntity.noContent().build();
    }
}

