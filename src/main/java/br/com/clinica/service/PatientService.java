package br.com.clinica.service;

import br.com.clinica.dto.request.PatientCreateRequest;
import br.com.clinica.dto.request.PatientUpdateRequest;
import br.com.clinica.dto.response.PatientResponse;
import br.com.clinica.entity.Address;
import br.com.clinica.entity.Patient;
import br.com.clinica.exception.BusinessException;
import br.com.clinica.exception.ResourceNotFoundException;
import br.com.clinica.repository.AddressRepository;
import br.com.clinica.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final AddressRepository addressRepository;

    @Transactional
    public PatientResponse create(PatientCreateRequest request) {
        if (patientRepository.existsByCpf(request.cpf())) {
            throw new BusinessException("Já existe um paciente cadastrado com este CPF.");
        }

        validateEmail(request.email(), null);
        Address address = findAddress(request.addressId());
        Patient patient = new Patient();
        patient.setName(request.name());
        patient.setCpf(request.cpf());
        patient.setDateBirth(request.dateBirth());
        patient.setTelephone(request.telephone());
        patient.setCellPhone(request.cellPhone());
        patient.setEmail(request.email());
        patient.setGender(request.gender());
        patient.setAddress(address);

        return toResponse(patientRepository.save(patient));
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> findAll() {
        return patientRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PatientResponse findById(UUID id) {
        return toResponse(findPatient(id));
    }

    @Transactional
    public PatientResponse update(UUID id, PatientUpdateRequest request) {
        Patient patient = findPatient(id);
        validateEmail(request.email(), id);
        Address address = findAddress(request.addressId());
        patient.setName(request.name());
        patient.setDateBirth(request.dateBirth());
        patient.setTelephone(request.telephone());
        patient.setCellPhone(request.cellPhone());
        patient.setEmail(request.email());
        patient.setGender(request.gender());
        patient.setAddress(address);

        return toResponse(patientRepository.save(patient));
    }

    @Transactional
    public void delete(UUID id) {
        patientRepository.delete(findPatient(id));
    }

    private Patient findPatient(UUID id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado: " + id));
    }

    private void validateEmail(String email, UUID id) {
        if (email == null || email.isBlank()) return;
        boolean exists = id == null ? patientRepository.existsByEmailIgnoreCase(email)
                : patientRepository.existsByEmailIgnoreCaseAndIdNot(email, id);
        if (exists) throw new BusinessException("Já existe um paciente cadastrado com este e-mail.");
    }

    private Address findAddress(UUID id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado: " + id));
    }

    private PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getName(),
                patient.getCpf(),
                patient.getDateBirth(),
                patient.getTelephone(),
                patient.getCellPhone(),
                patient.getEmail(),
                patient.getGender(),
                patient.getAddress().getId()
        );
    }
}
