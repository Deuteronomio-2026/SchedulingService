package edu.arsw.proyecto.SchedulingService.application.port.out;

import edu.arsw.proyecto.SchedulingService.domain.model.Session;

public interface EventPublisherPort {
    void publishSessionBooked(Session session);
    void publishSessionCancelled(Session session);
}