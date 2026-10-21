package br.com.clinica.service;

import br.com.clinica.dto.request.SpecialtyRequestDTO;
import br.com.clinica.dto.response.SpecialtyResponseDTO;
import br.com.clinica.entity.Specialty;
import br.com.clinica.exception.BusinessException;
import br.com.clinica.repository.SpecialtyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecialtyServiceTest {

    @Mock
    private SpecialtyRepository specialtyRepository;

    @InjectMocks
    private SpecialtyService specialtyService;

    @Test
    @DisplayName("Cenário: criar/listar/buscar/atualizar - operações retornam contrato esperado")
    void deveGarantirRetornoDoContratoEsperadoEmTodasAsOperacoes() {
        UUID id = UUID.randomUUID();
        Specialty specialty = new Specialty();
        specialty.setId(id);
        specialty.setName("Cardiologia");
        specialty.setDescription("Cuidados do coração");
        specialty.setCreated(OffsetDateTime.now());

        SpecialtyRequestDTO request = new SpecialtyRequestDTO("Cardiologia", "Cuidados do coração");

        // Mock para Create
        when(specialtyRepository.existsByName(request.name())).thenReturn(false);
        when(specialtyRepository.save(any(Specialty.class))).thenReturn(specialty);

        SpecialtyResponseDTO responseCreate = specialtyService.create(request);
        assertNotNull(responseCreate);
        assertEquals(id, responseCreate.id());

        // Mock para FindById
        when(specialtyRepository.findById(id)).thenReturn(Optional.of(specialty));
        SpecialtyResponseDTO responseFind = specialtyService.findById(id);
        assertNotNull(responseFind);
        assertEquals("Cardiologia", responseFind.name());

        // Mock para FindAll
        when(specialtyRepository.findAll()).thenReturn(List.of(specialty));
        List<SpecialtyResponseDTO> responseList = specialtyService.findAll();
        assertFalse(responseList.isEmpty());
        assertEquals(1, responseList.size());

        // Mock para Update
        when(specialtyRepository.save(any(Specialty.class))).thenReturn(specialty);
        SpecialtyResponseDTO responseUpdate = specialtyService.update(id, request);
        assertNotNull(responseUpdate);
        assertEquals("Cuidados do coração", responseUpdate.description());
    }

    @Test
    @DisplayName("Cenário: nome duplicado - erro de negócio")
    void deveFalharQuandoNomeDuplicado() {
        SpecialtyRequestDTO request = new SpecialtyRequestDTO("Cardiologia", "Descrição");
        when(specialtyRepository.existsByName(request.name())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> specialtyService.create(request));
        assertEquals("Já existe uma especialidade cadastrada com este nome.", exception.getMessage());
    }
}
