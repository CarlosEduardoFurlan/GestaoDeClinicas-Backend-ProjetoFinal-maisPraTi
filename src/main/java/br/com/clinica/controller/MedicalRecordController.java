package br.com.clinica.controller;

import br.com.clinica.dto.request.MedicalRecordCreateRequest;
import br.com.clinica.dto.request.MedicalRecordUpdateRequest;
import br.com.clinica.dto.response.MedicalRecordResponse;
import br.com.clinica.service.MedicalRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {
    private final MedicalRecordService medicalRecordService;

    @GetMapping
    public ResponseEntity<List<MedicalRecordResponse>> findAll() {
        return ResponseEntity.ok(medicalRecordService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalRecordResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(medicalRecordService.findById(id));
    }

    @PostMapping
    public ResponseEntity<MedicalRecordResponse> create(
            @Valid @RequestBody MedicalRecordCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicalRecordService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicalRecordResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody MedicalRecordUpdateRequest request) {
        return ResponseEntity.ok(medicalRecordService.update(id, request));
    }
}
