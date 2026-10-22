package br.com.clinica.service;

import br.com.clinica.dto.request.ScheduleRequestDTO;
import br.com.clinica.dto.response.ScheduleResponseDTO;
import br.com.clinica.entity.Schedule;
import br.com.clinica.exception.BusinessException;
import br.com.clinica.exception.ResourceNotFoundException;
import br.com.clinica.repository.ProfessionalRepository;
import br.com.clinica.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final ScheduleRepository schedules;
    private final ProfessionalRepository professionals;

    @Transactional
    public ScheduleResponseDTO create(ScheduleRequestDTO request) {
        var professional = professionals.findById(request.professionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Profissional não encontrado."));
        if (schedules.existsByProfessional_IdAndDateAndTime(request.professionalId(), request.date(), request.time())) {
            throw new BusinessException("Horário já cadastrado para este profissional.");
        }
        var slot = new Schedule();
        slot.setProfessional(professional);
        slot.setDate(request.date());
        slot.setTime(request.time());
        return toResponse(schedules.save(slot));
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponseDTO> findAll() {
        return schedules.findAll().stream().map(this::toResponse).toList();
    }

    private ScheduleResponseDTO toResponse(Schedule slot) {
        return new ScheduleResponseDTO(slot.getId(), slot.getProfessional().getId(),
                slot.getProfessional().getName(), slot.getDate(), slot.getTime(), slot.getCreated());
    }
}
