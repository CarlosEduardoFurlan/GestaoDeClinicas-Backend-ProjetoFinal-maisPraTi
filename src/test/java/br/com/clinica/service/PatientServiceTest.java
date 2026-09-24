package br.com.clinica.service;

import br.com.clinica.dto.request.*;
import br.com.clinica.entity.*;
import br.com.clinica.exception.*;
import br.com.clinica.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {
    @Mock PatientRepository patients;
    @Mock AddressRepository addresses;
    @InjectMocks PatientService service;
    Patient patient;
    Address address;
    PatientCreateRequest request;
    @BeforeEach void setup() {
        address = new Address(); address.setId(UUID.randomUUID());
        patient = new Patient(); patient.setId(UUID.randomUUID()); patient.setName("Maria");
        patient.setCpf("12345678901"); patient.setAddress(address);
        request = new PatientCreateRequest("Maria", "12345678901", LocalDate.of(1990,5,20), null,
                "51999999999", "maria@example.com", "F", address.getId());
    }
    @Test void createsPatientWithAddress() {
        when(addresses.findById(address.getId())).thenReturn(Optional.of(address));
        when(patients.save(any())).thenAnswer(call -> { Patient p=call.getArgument(0); p.setId(patient.getId()); return p; });
        var result=service.create(request);
        assertEquals(patient.getId(), result.id()); assertEquals(address.getId(), result.addressId());
        assertEquals(request.dateBirth(), result.dateBirth());
    }
    @Test void rejectsDuplicateCpf() {
        when(patients.existsByCpf(request.cpf())).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.create(request)); verify(patients, never()).save(any());
    }
    @Test void rejectsDuplicateEmail() {
        when(patients.existsByEmailIgnoreCase(request.email())).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.create(request)); verify(patients, never()).save(any());
    }
    @Test void rejectsMissingAddress() {
        assertThrows(ResourceNotFoundException.class, () -> service.create(request)); verify(patients, never()).save(any());
    }
    @Test void findsPatient() {
        when(patients.findById(patient.getId())).thenReturn(Optional.of(patient));
        assertEquals(patient.getId(), service.findById(patient.getId()).id());
    }
    @Test void rejectsMissingPatient() { assertThrows(ResourceNotFoundException.class, () -> service.findById(patient.getId())); }
    @Test void listsPatients() {
        when(patients.findAll()).thenReturn(List.of(patient)); assertEquals(1, service.findAll().size());
    }
    @Test void updatesWithoutChangingCpf() {
        when(patients.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(addresses.findById(address.getId())).thenReturn(Optional.of(address));
        when(patients.save(any())).thenAnswer(call -> call.getArgument(0));
        var update=new PatientUpdateRequest("Maria Nova", request.dateBirth(), null, request.cellPhone(), request.email(), "F", address.getId());
        var result=service.update(patient.getId(),update);
        assertEquals("Maria Nova",result.name()); assertEquals(patient.getCpf(),result.cpf());
        verify(patients).existsByEmailIgnoreCaseAndIdNot(request.email(),patient.getId());
    }
    @Test void rejectsEmailOfAnotherPatientOnUpdate() {
        when(patients.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(patients.existsByEmailIgnoreCaseAndIdNot(request.email(),patient.getId())).thenReturn(true);
        var update=new PatientUpdateRequest("Maria Nova",request.dateBirth(),null,request.cellPhone(),request.email(),"F",address.getId());
        assertThrows(BusinessException.class,()->service.update(patient.getId(),update)); verify(patients,never()).save(any());
    }
    @Test void deletesExistingPatient() {
        when(patients.findById(patient.getId())).thenReturn(Optional.of(patient)); service.delete(patient.getId()); verify(patients).delete(patient);
    }
    @Test void rejectsMissingPatientOnDelete() {
        assertThrows(ResourceNotFoundException.class,()->service.delete(patient.getId())); verify(patients,never()).delete(any());
    }
}
