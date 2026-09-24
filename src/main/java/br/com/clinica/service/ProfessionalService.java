package br.com.clinica.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import br.com.clinica.exception.BusinessException;
import br.com.clinica.exception.ResourceNotFoundException;

import br.com.clinica.dto.request.ProfessionalRequestDTO;
import br.com.clinica.dto.response.ProfessionalResponseDTO;
import br.com.clinica.entity.Address;
import br.com.clinica.entity.Professional;
import br.com.clinica.entity.Specialty;
import br.com.clinica.entity.User;
import br.com.clinica.repository.AddressRepository;
import br.com.clinica.repository.ProfessionalRepository;
import br.com.clinica.repository.SpecialtyRepository;
import br.com.clinica.repository.UserRepository;

@Service
public class ProfessionalService {

    private final ProfessionalRepository professionalRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final SpecialtyRepository specialtyRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfessionalService(ProfessionalRepository professionalRepository,
                               UserRepository userRepository,
                               AddressRepository addressRepository,
                               SpecialtyRepository specialtyRepository, PasswordEncoder passwordEncoder) {
        this.professionalRepository = professionalRepository;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.specialtyRepository = specialtyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ProfessionalResponseDTO create(ProfessionalRequestDTO dto) {
        // 1. Validações de duplicidade
        if (professionalRepository.existsByRegister(dto.register())) {
            throw new BusinessException("Registro profissional já cadastrado.");
        }
        if (userRepository.existsByEmail(dto.user().email())) {
            throw new BusinessException("E-mail já cadastrado.");
        }

        // 2. Buscar Especialidade existente
        Specialty specialty = specialtyRepository.findById(dto.specialtyId())
                .orElseThrow(() -> new ResourceNotFoundException("Especialidade não encontrada."));

        // 3. Criar e salvar Usuário
        User user = new User();
        user.setName(dto.user().name());
        user.setEmail(dto.user().email());
        user.setPassword(passwordEncoder.encode(dto.user().password()));
        user.setProfile(dto.user().profile());
        user = userRepository.save(user);

        // 4. Criar e salvar Endereço
        Address address = new Address();
        address.setAddress(dto.address().address());
        address.setNumber(dto.address().number());
        address.setComplement(dto.address().complement());
        address.setDistrict(dto.address().district());
        address.setCity(dto.address().city());
        address.setUf(dto.address().uf());
        address.setZipCode(dto.address().zipCode());
        address = addressRepository.save(address);

        // 5. Criar e salvar Profissional
        Professional professional = new Professional();
        professional.setName(dto.name());
        professional.setRegister(dto.register());
        professional.setTelephone(dto.telephone());
        professional.setCellPhone(dto.cellPhone());
        professional.setEmail(dto.email());
        professional.setSpecialty(specialty);
        professional.setAddress(address);
        professional.setUser(user);

        Professional saved = professionalRepository.save(professional);

        // 6. Retornar DTO de resposta
        return toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ProfessionalResponseDTO> findAll() {
        return professionalRepository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfessionalResponseDTO findById(UUID id) {
        Professional professional = professionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profissional não encontrado."));
        return toResponseDTO(professional);
    }

    // Método auxiliar de conversão de Entidade para DTO
    private ProfessionalResponseDTO toResponseDTO(Professional entity) {
        return new ProfessionalResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getRegister(),
                entity.getTelephone(),
                entity.getCellPhone(),
                entity.getEmail(),
                entity.getSpecialty() != null ? entity.getSpecialty().getName() : null,
                entity.getAddress() != null ? entity.getAddress().getCity() : null,
                entity.getUser() != null ? entity.getUser().getId() : null
        );
    }
}
