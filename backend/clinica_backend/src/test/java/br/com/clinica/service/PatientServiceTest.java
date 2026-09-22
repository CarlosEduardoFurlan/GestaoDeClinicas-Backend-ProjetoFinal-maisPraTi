package br.com.clinica.service;

import br.com.clinica.dto.PatientCreateRequest;
import br.com.clinica.dto.PatientResponse;
import br.com.clinica.dto.PatientUpdateRequest;
import br.com.clinica.entity.Patient;
import br.com.clinica.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientService patientService;

    private Patient patient;
    private PatientCreateRequest createRequest;
    private PatientUpdateRequest updateRequest;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        
        patient = Patient.builder()
                .id(patientId)
                .name("Maria Silva")
                .cpf("12345678901")
                .email("maria@email.com")
                .phone("51999999999")
                .birthDate(LocalDate.of(1990, 5, 20))
                .build();

        createRequest = PatientCreateRequest.builder()
                .name("Maria Silva")
                .cpf("12345678901")
                .email("maria@email.com")
                .phone("51999999999")
                .birthDate(LocalDate.of(1990, 5, 20))
                .build();

        updateRequest = PatientUpdateRequest.builder()
                .name("Maria Silva Alterada")
                .email("maria.nova@email.com")
                .phone("51988888888")
                .birthDate(LocalDate.of(1990, 5, 20))
                .build();
    }

    @Test
    @DisplayName("Deve criar um paciente com sucesso")
    void create_ShouldCreatePatient_WhenValidData() {
        when(patientRepository.existsByCpf(anyString())).thenReturn(false);
        when(patientRepository.existsByEmail(anyString())).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);

        PatientResponse response = patientService.create(createRequest);

        assertNotNull(response);
        assertEquals(patientId, response.getId());
        assertEquals("Maria Silva", response.getName());
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar criar paciente com CPF duplicado")
    void create_ShouldThrowException_WhenCpfExists() {
        when(patientRepository.existsByCpf(createRequest.getCpf())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            patientService.create(createRequest);
        });

        assertEquals("Já existe um paciente cadastrado com este CPF.", exception.getMessage());
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("Deve buscar paciente por ID com sucesso")
    void findById_ShouldReturnPatient_WhenExists() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        PatientResponse response = patientService.findById(patientId);

        assertNotNull(response);
        assertEquals(patientId, response.getId());
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar paciente por ID inexistente")
    void findById_ShouldThrowException_WhenNotFound() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            patientService.findById(patientId);
        });
    }

    @Test
    @DisplayName("Deve listar todos os pacientes")
    void findAll_ShouldReturnList() {
        when(patientRepository.findAll()).thenReturn(List.of(patient));

        List<PatientResponse> list = patientService.findAll();

        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("Deve deletar paciente por ID com sucesso")
    void delete_ShouldDelete_WhenExists() {
        when(patientRepository.existsById(patientId)).thenReturn(true);
        doNothing().when(patientRepository).deleteById(patientId);

        assertDoesNotThrow(() -> patientService.delete(patientId));
        verify(patientRepository, times(1)).deleteById(patientId);
    }
}