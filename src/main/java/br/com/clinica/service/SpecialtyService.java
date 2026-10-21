package br.com.clinica.service;

import br.com.clinica.dto.request.SpecialtyRequestDTO;
import br.com.clinica.dto.response.SpecialtyResponseDTO;
import br.com.clinica.entity.Specialty;
import br.com.clinica.exception.BusinessException; // Seu padrão de erro (ex: 400 Bad Request)
import br.com.clinica.exception.ResourceNotFoundException; // Seu padrão de erro (ex: 404 Not Found)
import br.com.clinica.repository.SpecialtyRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpecialtyService {

    private final SpecialtyRepository specialtyRepository;

    // 1. Criar Especialidade (Validando nome único)
    @Transactional
    public SpecialtyResponseDTO create(SpecialtyRequestDTO request) {
        if (specialtyRepository.existsByName(request.name())) {
            throw new BusinessException("Já existe uma especialidade cadastrada com este nome.");
        }

        Specialty specialty = new Specialty();
        specialty.setName(request.name());
        specialty.setDescription(request.description());

        Specialty savedSpecialty = specialtyRepository.save(specialty);
        return mapToResponse(savedSpecialty);
    }

    // 2. Listagem Geral
    @Transactional(readOnly = true)
    public List<SpecialtyResponseDTO> findAll() {
        return specialtyRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 3. Busca por ID
    @Transactional(readOnly = true)
    public SpecialtyResponseDTO findById(UUID id) {
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidade não encontrada com o ID: " + id));
        return mapToResponse(specialty);
    }

    // 4. Atualização (Sem introduzir novos campos)
    @Transactional
    public SpecialtyResponseDTO update(UUID id, SpecialtyRequestDTO request) {
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidade não encontrada com o ID: " + id));

        // Valida se o novo nome já está em uso por OUTRA especialidade
        if (!specialty.getName().equals(request.name()) && specialtyRepository.existsByName(request.name())) {
            throw new BusinessException("Já existe outra especialidade cadastrada com este nome.");
        }

        specialty.setName(request.name());
        specialty.setDescription(request.description());

        Specialty updatedSpecialty = specialtyRepository.save(specialty);
        return mapToResponse(updatedSpecialty);
    }

    // 5. Exclusão Respeitando Vínculos (Sem Cascade)
    @Transactional
    public void delete(UUID id) {
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidade não encontrada com o ID: " + id));

        specialtyRepository.delete(specialty);
        specialtyRepository.flush();
    }

    // Mapper auxiliar interno
    private SpecialtyResponseDTO mapToResponse(Specialty specialty) {
        return new SpecialtyResponseDTO(
                specialty.getId(),
                specialty.getName(),
                specialty.getDescription(),
                specialty.getCreated()
        );
    }
}
