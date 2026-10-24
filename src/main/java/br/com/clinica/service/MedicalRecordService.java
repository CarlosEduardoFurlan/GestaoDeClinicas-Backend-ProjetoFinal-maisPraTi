package br.com.clinica.service;

import br.com.clinica.dto.request.MedicalRecordCreateRequest;
import br.com.clinica.dto.request.MedicalRecordUpdateRequest;
import br.com.clinica.dto.response.MedicalRecordResponse;
import br.com.clinica.entity.MedicalRecord;
import br.com.clinica.exception.ConflictException;
import br.com.clinica.exception.ResourceNotFoundException;
import br.com.clinica.repository.AppointmentRepository;
import br.com.clinica.repository.MedicalRecordRepository;
import br.com.clinica.security.CurrentUserAccess;
import br.com.clinica.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicalRecordService {
    private final MedicalRecordRepository medicalRecords;
    private final AppointmentRepository appointments;
    private final CurrentUserAccess access;

    @Transactional
    public MedicalRecordResponse create(MedicalRecordCreateRequest request) {
        if (access.role() != Role.PROFESSIONAL) {
            throw new AccessDeniedException("Somente Professional pode criar prontuário.");
        }
        var appointment = appointments.findById(request.appointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado."));
        access.requireOwnProfessional(appointment.getProfessional().getId());
        if (medicalRecords.existsByAppointment_Id(request.appointmentId())) {
            throw new ConflictException("O agendamento já possui prontuário.");
        }
        var medicalRecord = new MedicalRecord();
        medicalRecord.setAppointment(appointment);
        medicalRecord.setNotes(request.notes());
        return toResponse(medicalRecords.save(medicalRecord));
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> findAll() {
        var role = access.role();
        if (role == Role.RECEPTIONIST) {
            throw new AccessDeniedException("Recepção não pode acessar prontuários.");
        }
        var result = role == Role.PROFESSIONAL
                ? medicalRecords.findAllByAppointment_Professional_Id(access.professionalId())
                : medicalRecords.findAll();
        return result.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MedicalRecordResponse findById(UUID id) {
        var medicalRecord = findMedicalRecord(id);
        validateReadOwnership(medicalRecord);
        return toResponse(medicalRecord);
    }

    @Transactional
    public MedicalRecordResponse update(UUID id, MedicalRecordUpdateRequest request) {
        if (access.role() != Role.PROFESSIONAL) {
            throw new AccessDeniedException("Somente Professional pode alterar prontuário.");
        }
        var medicalRecord = findMedicalRecord(id);
        access.requireOwnProfessional(medicalRecord.getAppointment().getProfessional().getId());
        medicalRecord.setNotes(request.notes());
        return toResponse(medicalRecords.save(medicalRecord));
    }

    private MedicalRecord findMedicalRecord(UUID id) {
        return medicalRecords.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prontuário não encontrado."));
    }

    private void validateReadOwnership(MedicalRecord medicalRecord) {
        var role = access.role();
        if (role == Role.RECEPTIONIST) {
            throw new AccessDeniedException("Recepção não pode acessar prontuários.");
        }
        if (role == Role.PROFESSIONAL) {
            access.requireOwnProfessional(medicalRecord.getAppointment().getProfessional().getId());
        }
    }

    private MedicalRecordResponse toResponse(MedicalRecord medicalRecord) {
        return new MedicalRecordResponse(medicalRecord.getId(),
                medicalRecord.getAppointment().getId(), medicalRecord.getNotes());
    }
}
