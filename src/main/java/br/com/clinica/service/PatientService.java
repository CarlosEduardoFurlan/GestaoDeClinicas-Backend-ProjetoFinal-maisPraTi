package br.com.clinica.service;

import br.com.clinica.dto.request.PatientCreateRequest;
import br.com.clinica.dto.request.PatientUpdateRequest;
import br.com.clinica.dto.request.AddressRequestDTO;
import br.com.clinica.dto.response.PatientResponse;
import br.com.clinica.dto.response.AddressResponseDTO;
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
        Address address = new Address();
        updateAddress(address, request.address());
        address = addressRepository.save(address);
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
        Address address = patient.getAddress();
        if (address == null) {
            throw new ResourceNotFoundException("Endereço associado ao paciente não encontrado.");
        }
        updateAddress(address, request.address());
        addressRepository.save(address);
        patient.setName(request.name());
        patient.setDateBirth(request.dateBirth());
        patient.setTelephone(request.telephone());
        patient.setCellPhone(request.cellPhone());
        patient.setEmail(request.email());
        patient.setGender(request.gender());

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

    private void updateAddress(Address address, AddressRequestDTO request) {
        address.setAddress(request.address());
        address.setNumber(request.number());
        address.setComplement(request.complement());
        address.setDistrict(request.district());
        address.setCity(request.city());
        address.setUf(request.uf());
        address.setZipCode(request.zipCode());
    }

    private AddressResponseDTO toAddressResponse(Address address) {
        return new AddressResponseDTO(address.getAddress(), address.getNumber(), address.getComplement(),
                address.getDistrict(), address.getCity(), address.getUf(), address.getZipCode());
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
                patient.getAddress().getId(),
                toAddressResponse(patient.getAddress())
        );
    }
}
