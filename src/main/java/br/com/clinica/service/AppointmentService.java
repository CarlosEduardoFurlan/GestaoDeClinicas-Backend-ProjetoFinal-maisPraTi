package br.com.clinica.service;

import br.com.clinica.dto.request.AppointmentRequestDTO;
import br.com.clinica.dto.request.AppointmentStatusUpdateRequestDTO;
import br.com.clinica.dto.response.AppointmentResponseDTO;
import br.com.clinica.entity.Appointment;
import br.com.clinica.entity.Status;
import br.com.clinica.exception.BusinessException;
import br.com.clinica.exception.ConflictException;
import br.com.clinica.exception.ResourceNotFoundException;
import br.com.clinica.repository.AppointmentRepository;
import br.com.clinica.repository.PatientRepository;
import br.com.clinica.repository.ProfessionalRepository;
import br.com.clinica.repository.ScheduleRepository;
import br.com.clinica.repository.StatusRepository;
import br.com.clinica.security.CurrentUserAccess;
import br.com.clinica.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {
    static final String SCHEDULED = "Agendada";
    static final String CONFIRMED = "Confirmada";
    static final String IN_PROGRESS = "Em atendimento";
    static final String COMPLETED = "Concluída";
    static final String CANCELLED = "Cancelada";

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            SCHEDULED, Set.of(CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(IN_PROGRESS, CANCELLED),
            IN_PROGRESS, Set.of(COMPLETED),
            COMPLETED, Set.of(),
            CANCELLED, Set.of());

    private final AppointmentRepository appointments;
    private final PatientRepository patients;
    private final ProfessionalRepository professionals;
    private final ScheduleRepository schedules;
    private final StatusRepository statuses;
    private final CurrentUserAccess access;

    @Transactional
    public AppointmentResponseDTO create(AppointmentRequestDTO request) {
        if (access.role() == Role.PROFESSIONAL) {
            throw new AccessDeniedException("Professional não pode criar agendamentos.");
        }
        var patient = patients.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado."));
        var professional = professionals.findById(request.professionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Profissional não encontrado."));

        schedules.findSlotForUpdate(request.professionalId(), request.date(), request.time())
                .orElseThrow(() -> new ResourceNotFoundException("Horário não disponível na agenda do profissional."));

        if (appointments.existsByProfessional_IdAndDateAndTimeAndStatus_DescriptionNot(
                request.professionalId(), request.date(), request.time(), CANCELLED)) {
            throw new ConflictException("Horário já reservado para este profissional.");
        }

        var appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setProfessional(professional);
        appointment.setDate(request.date());
        appointment.setTime(request.time());
        appointment.setStatus(findStatusByDescription(SCHEDULED));
        appointment.setNotes(request.notes());
        return toResponse(appointments.save(appointment));
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> findAll() {
        var result = access.role() == Role.PROFESSIONAL
                ? appointments.findAllByProfessional_Id(access.professionalId())
                : appointments.findAll();
        return result.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponseDTO findById(UUID id) {
        var appointment = findAppointment(id);
        requireAppointmentOwnershipWhenProfessional(appointment);
        return toResponse(appointment);
    }

    @Transactional
    public AppointmentResponseDTO updateStatus(UUID id, AppointmentStatusUpdateRequestDTO request) {
        var appointment = findAppointment(id);
        requireAppointmentOwnershipWhenProfessional(appointment);
        var targetStatus = statuses.findById(request.statusId())
                .orElseThrow(() -> new ResourceNotFoundException("Status não encontrado."));
        validateTransition(appointment.getStatus(), targetStatus);
        validateActorTransition(appointment, targetStatus);
        appointment.setStatus(targetStatus);
        return toResponse(appointments.save(appointment));
    }

    @Transactional
    public void cancel(UUID id) {
        if (access.role() == Role.PROFESSIONAL) {
            throw new AccessDeniedException("Professional não pode cancelar agendamentos.");
        }
        var appointment = findAppointment(id);
        var cancelled = findStatusByDescription(CANCELLED);
        validateTransition(appointment.getStatus(), cancelled);
        appointment.setStatus(cancelled);
        appointments.save(appointment);
    }

    private Appointment findAppointment(UUID id) {
        return appointments.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado."));
    }

    private Status findStatusByDescription(String description) {
        return statuses.findByDescription(description)
                .orElseThrow(() -> new IllegalStateException("Catálogo oficial de status não inicializado: " + description));
    }

    private void validateTransition(Status current, Status target) {
        var allowedTargets = ALLOWED_TRANSITIONS.get(current.getDescription());
        if (allowedTargets == null || !allowedTargets.contains(target.getDescription())) {
            throw new BusinessException("Transição de status não permitida: "
                    + current.getDescription() + " -> " + target.getDescription() + ".");
        }
    }

    private void validateActorTransition(Appointment appointment, Status target) {
        var role = access.role();
        if (role == Role.ADMIN) {
            return;
        }
        var current = appointment.getStatus().getDescription();
        var targetDescription = target.getDescription();
        if (role == Role.RECEPTIONIST) {
            if (SCHEDULED.equals(current) && CONFIRMED.equals(targetDescription)) {
                return;
            }
            throw new AccessDeniedException("Recepção não pode executar esta transição de status.");
        }
        if ((CONFIRMED.equals(current) && IN_PROGRESS.equals(targetDescription))
                || (IN_PROGRESS.equals(current) && COMPLETED.equals(targetDescription))) {
            return;
        }
        throw new AccessDeniedException("Professional não pode executar esta transição de status.");
    }

    private void requireAppointmentOwnershipWhenProfessional(Appointment appointment) {
        if (access.role() == Role.PROFESSIONAL) {
            access.requireOwnProfessional(appointment.getProfessional().getId());
        }
    }

    private AppointmentResponseDTO toResponse(Appointment appointment) {
        return new AppointmentResponseDTO(appointment.getId(), appointment.getPatient().getId(),
                appointment.getPatient().getName(), appointment.getProfessional().getId(),
                appointment.getProfessional().getName(), appointment.getDate(), appointment.getTime(),
                appointment.getStatus().getId(), appointment.getStatus().getDescription(),
                appointment.getNotes(), appointment.getCreated());
    }
}
