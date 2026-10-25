package br.com.clinica.controller;

import br.com.clinica.dto.response.StatusResponseDTO;
import br.com.clinica.service.StatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/statuses")
@RequiredArgsConstructor
public class StatusController {
    private final StatusService statusService;

    @GetMapping
    public ResponseEntity<List<StatusResponseDTO>> findAll() {
        return ResponseEntity.ok(statusService.findAll());
    }
}
