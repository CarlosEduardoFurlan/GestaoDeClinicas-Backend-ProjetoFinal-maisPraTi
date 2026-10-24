package br.com.clinica.controller;

import br.com.clinica.dto.request.ScheduleRequestDTO;
import br.com.clinica.dto.response.ScheduleResponseDTO;
import br.com.clinica.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {
    private final ScheduleService scheduleService;

    @GetMapping
    public ResponseEntity<List<ScheduleResponseDTO>> findAll() {
        return ResponseEntity.ok(scheduleService.findAll());
    }

    @PostMapping
    public ResponseEntity<ScheduleResponseDTO> create(@Valid @RequestBody ScheduleRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.create(request));
    }
}
