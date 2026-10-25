package br.com.clinica.service;

import br.com.clinica.entity.Status;
import br.com.clinica.repository.StatusRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatusServiceTest {
    private final StatusRepository repository = mock(StatusRepository.class);
    private final StatusService service = new StatusService(repository);

    @Test
    void returnsStatusDtos() {
        var status = new Status(UUID.randomUUID(), "Agendada");
        when(repository.findAll()).thenReturn(List.of(status));
        var response = service.findAll();
        assertEquals(status.getId(), response.getFirst().id());
        assertEquals("Agendada", response.getFirst().description());
    }
}
