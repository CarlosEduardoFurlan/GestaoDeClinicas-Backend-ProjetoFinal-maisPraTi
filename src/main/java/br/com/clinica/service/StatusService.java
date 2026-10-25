package br.com.clinica.service;

import br.com.clinica.dto.response.StatusResponseDTO;
import br.com.clinica.repository.StatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatusService {
    private final StatusRepository statusRepository;

    @Transactional(readOnly = true)
    public List<StatusResponseDTO> findAll() {
        return statusRepository.findAll().stream()
                .map(status -> new StatusResponseDTO(status.getId(), status.getDescription()))
                .toList();
    }
}
