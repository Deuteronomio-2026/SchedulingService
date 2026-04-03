package edu.arsw.proyecto.SchedulingService.application.port.in;

import edu.arsw.proyecto.SchedulingService.application.dto.BookSessionDTO;
import edu.arsw.proyecto.SchedulingService.domain.model.Session;
import java.util.List;
import java.util.UUID;

public interface BookSessionUseCase {
    Session book(BookSessionDTO command);
    void cancel(UUID sessionId);
    List<Session> findAll();
}