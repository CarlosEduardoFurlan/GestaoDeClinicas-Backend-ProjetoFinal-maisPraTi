package br.com.clinica.service;

import br.com.clinica.dto.request.MedicalRecordCreateRequest;
import br.com.clinica.dto.request.MedicalRecordUpdateRequest;
import br.com.clinica.dto.response.MedicalRecordResponse;
import br.com.clinica.entity.MedicalRecord;
import br.com.clinica.exception.ConflictException;
import br.com.clinica.exception.ResourceNotFoundException;
import br.com.clinica.repository.AppointmentRepository;
import br.com.clinica.repository.MedicalRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicalRecordService {
    private final MedicalRecordRepository medicalRecords;
    private final AppointmentRepository appointments;

    @Transactional
    public MedicalRecordResponse create(MedicalRecordCreateRequest request) {
        var appointment = appointments.findById(request.appointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado."));
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
        return medicalRecords.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MedicalRecordResponse findById(UUID id) {
        return toResponse(findMedicalRecord(id));
    }

    @Transactional
    public MedicalRecordResponse update(UUID id, MedicalRecordUpdateRequest request) {
        var medicalRecord = findMedicalRecord(id);
        medicalRecord.setNotes(request.notes());
        return toResponse(medicalRecords.save(medicalRecord));
    }

    private MedicalRecord findMedicalRecord(UUID id) {
        return medicalRecords.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prontuário não encontrado."));
    }

    private MedicalRecordResponse toResponse(MedicalRecord medicalRecord) {
        return new MedicalRecordResponse(medicalRecord.getId(),
                medicalRecord.getAppointment().getId(), medicalRecord.getNotes());
    }
}
