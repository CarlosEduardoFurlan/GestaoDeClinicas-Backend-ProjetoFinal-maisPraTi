package br.com.clinica.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.clinica.dto.PatientCreateRequest;
import br.com.clinica.dto.PatientResponse;
import br.com.clinica.dto.PatientUpdateRequest;
import br.com.clinica.entity.Patient;
import br.com.clinica.repository.PatientRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    @Transactional
    public PatientResponse create(PatientCreateRequest request) {
        if (patientRepository.existsByCpf(request.getCpf())) {
            throw new IllegalArgumentException("Já existe um paciente cadastrado com este CPF.");
        }

        if (patientRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Já existe um paciente cadastrado com este e-mail.");
        }

        Patient patient = Patient.builder()
                .name(request.getName())
                .cpf(request.getCpf())
                .email(request.getEmail())
                .phone(request.getPhone())
                .birthDate(request.getBirthDate())
                .build();

        Patient savedPatient = patientRepository.save(patient);

        return toResponse(savedPatient);
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> findAll() {
        return patientRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PatientResponse findById(UUID id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado com o ID: " + id));
        return toResponse(patient);
    }

    @Transactional
    public PatientResponse update(UUID id, PatientUpdateRequest request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado com o ID: " + id));

        // Verificar se o novo e-mail já está em uso por outro paciente
        if (!patient.getEmail().equalsIgnoreCase(request.getEmail()) &&
                patientRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Este e-mail já está a ser utilizado por outro paciente.");
        }

        patient.setName(request.getName());
        patient.setEmail(request.getEmail());
        patient.setPhone(request.getPhone());
        patient.setBirthDate(request.getBirthDate());

        Patient updatedPatient = patientRepository.save(patient);
        return toResponse(updatedPatient);
    }

    @Transactional
    public void delete(UUID id) {
        if (!patientRepository.existsById(id)) {
            throw new IllegalArgumentException("Paciente não encontrado com o ID: " + id);
        }
        patientRepository.deleteById(id);
    }

    // Método utilitário privado para conversão de Entidade para DTO de Resposta
    private PatientResponse toResponse(Patient patient) {
        return PatientResponse.builder()
                .id(patient.getId())
                .name(patient.getName())
                .cpf(patient.getCpf())
                .email(patient.getEmail())
                .phone(patient.getPhone())
                .birthDate(patient.getBirthDate())
                .build();
    }
}