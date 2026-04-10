package edu.arsw.proyecto.SchedulingService.application.port.out;

import edu.arsw.proyecto.SchedulingService.domain.model.Session;
import edu.arsw.proyecto.SchedulingService.domain.model.TimeSlot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepositoryPort {
    Session save(Session session);
    Optional<Session> findById(UUID id);
    boolean existsByPsychologistAndSlot(UUID psychologistId, TimeSlot slot);
    void deleteAll();
    List<Session> findAll();
}