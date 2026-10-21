package br.com.clinica.service;

import br.com.clinica.dto.response.AppointmentResponseDTO;
import br.com.clinica.entity.Appointment;
import br.com.clinica.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {
    private final AppointmentRepository appointments;

    // Escritas dependem das decisões de status, cancelamento e proteção de dupla reserva.
    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> findAll() {
        return appointments.findAll().stream().map(this::toResponse).toList();
    }

    private AppointmentResponseDTO toResponse(Appointment appointment) {
        return new AppointmentResponseDTO(appointment.getId(), appointment.getPatient().getId(),
                appointment.getPatient().getName(), appointment.getProfessional().getId(),
                appointment.getProfessional().getName(), appointment.getDate(), appointment.getTime(),
                appointment.getStatus().getId(), appointment.getStatus().getDescription(),
                appointment.getNotes(), appointment.getCreated());
    }
}
