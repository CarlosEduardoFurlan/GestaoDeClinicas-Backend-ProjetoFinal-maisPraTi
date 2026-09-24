package br.com.clinica.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.clinica.dto.request.ProfessionalRequestDTO;
import br.com.clinica.dto.response.ProfessionalResponseDTO;
import br.com.clinica.service.ProfessionalService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/professionals")
public class ProfessionalController {

    private final ProfessionalService professionalService;

    public ProfessionalController(ProfessionalService professionalService) {
        this.professionalService = professionalService;
    }

    @PostMapping
    public ResponseEntity<ProfessionalResponseDTO> create(@RequestBody @Valid ProfessionalRequestDTO dto) {
        ProfessionalResponseDTO response = professionalService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProfessionalResponseDTO>> findAll() {
        return ResponseEntity.ok(professionalService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfessionalResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(professionalService.findById(id));
    }
}
